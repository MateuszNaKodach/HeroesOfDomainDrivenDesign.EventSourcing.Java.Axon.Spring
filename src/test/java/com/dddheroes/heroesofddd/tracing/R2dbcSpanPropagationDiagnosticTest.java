package com.dddheroes.heroesofddd.tracing;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures.BuiltDwellingReadModel;
import com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures.BuiltDwellingReadModelRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Diagnostic tests for Spring Boot's {@code R2dbcObservationAutoConfiguration} and {@code r2dbc-proxy}: verify that a
 * real Postgres query executed through the app's actual {@code ConnectionFactory} nests under trace context carried
 * in the <b>Reactor Context</b>. One test exercises Axoniq Framework's raw Micrometer span carrier and its balanced
 * nested-scope restoration; the other verifies Spring's native Observation carrier independently.
 * <p>
 * The {@code findById().switchIfEmpty(save()).then()} shape matters: the {@code save()} subscription happens on the
 * Postgres driver's Netty event-loop thread (where thread-local state is least reliable), which is where orphaned
 * spans were observed with the previous raw-OpenTelemetry instrumentation.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "management.tracing.enabled=true",
                "management.tracing.sampling.probability=1.0",
                "management.observations.enable.r2dbc=true",
                "spring.reactor.context-propagation=auto",
                "jdbc.datasource-proxy.enabled=false"
        })
class R2dbcSpanPropagationDiagnosticTest {

    @Autowired
    Tracer tracer;

    @Autowired
    ObservationRegistry observationRegistry;

    @Autowired
    BuiltDwellingReadModelRepository repository;

    @Autowired
    CollectingSpanExporter spanExporter;

    @BeforeEach
    void setUp() {
        Hooks.enableAutomaticContextPropagation();
        spanExporter.spans.clear();
    }

    @Test
    void r2dbcQueryNestsUnderTheRawMicrometerSpanCarriedInReactorContext() {
        Span parentSpan = tracer.nextSpan().name("test.raw-parent").start();
        try {
            runRepositoryPipeline(ctx -> ctx.put(ObservationAwareSpanThreadLocalAccessor.KEY, parentSpan));
        } finally {
            parentSpan.end();
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<SpanData> spans = List.copyOf(spanExporter.spans);
            SpanData parentData = spans.stream()
                                       .filter(s -> s.getName().equals("test.raw-parent"))
                                       .findFirst()
                                       .orElseThrow(() -> new AssertionError(
                                               "'test.raw-parent' not exported yet. Recorded: "
                                                       + spans.stream().map(SpanData::getName).toList()));
            List<SpanData> queryChildren = spans.stream()
                                                .filter(s -> s.getName().equals("query"))
                                                .toList();
            assertThat(queryChildren).isNotEmpty();
            assertThat(queryChildren).allSatisfy(child -> {
                assertThat(child.getTraceId()).isEqualTo(parentData.getTraceId());
                assertThat(child.getParentSpanContext().isValid()).isTrue();
            });
        });
    }

    @Test
    void r2dbcQueryNestsUnderAnObservationThatCarriesItsOwnSpan() {
        // given an Observation whose TracingContext carries a span (the shape Boot's WebFlux server observation
        // has), carried in the Reactor Context
        Observation parentObservation = Observation.start("test.parent", observationRegistry);
        try {
            runRepositoryPipeline(ctx -> ctx.put(ObservationThreadLocalAccessor.KEY, parentObservation));
        } finally {
            parentObservation.stop();
        }

        // then every r2dbc query span joins the observation's trace with a valid parent (it may nest one level
        // deeper, under an intermediate connection-scope span, but is never an orphaned root)
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<SpanData> spans = List.copyOf(spanExporter.spans);
            SpanData parentData = spans.stream()
                                        .filter(s -> s.getName().equals("test.parent"))
                                        .findFirst()
                                        .orElseThrow(() -> new AssertionError(
                                                "'test.parent' not exported yet. Recorded: "
                                                        + spans.stream().map(SpanData::getName).toList()));
            List<SpanData> queryChildren = spans.stream()
                                                 .filter(s -> s.getName().equals("query"))
                                                 .toList();
            assertThat(queryChildren).isNotEmpty();
            assertThat(queryChildren).allSatisfy(child -> {
                assertThat(child.getTraceId()).isEqualTo(parentData.getTraceId());
                assertThat(child.getParentSpanContext().isValid()).isTrue();
            });
        });
    }

    private void runRepositoryPipeline(UnaryOperator<reactor.util.context.Context> contextWriter) {
        String dwellingId = "diag-" + UUID.randomUUID();
        Mono<Void> operation = Mono.just(dwellingId)
                                   // hop off the caller thread first, like an Axon worker handing off
                                   .publishOn(Schedulers.boundedElastic())
                                   .flatMap(id -> repository.findById(id)
                                                            .switchIfEmpty(repository.save(
                                                                    new BuiltDwellingReadModel(
                                                                            "game-1", id, "creature-1")))
                                                            .then())
                                   .contextWrite(contextWriter::apply);
        operation.block(Duration.ofSeconds(10));
    }

    /** Minimal in-memory {@link SpanExporter} so this diagnostic test needs no extra test dependency. */
    static final class CollectingSpanExporter implements SpanExporter {

        final List<SpanData> spans = new CopyOnWriteArrayList<>();

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            this.spans.addAll(spans);
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class InMemorySpanExporterConfiguration {

        @Bean
        CollectingSpanExporter collectingSpanExporter() {
            return new CollectingSpanExporter();
        }
    }
}
