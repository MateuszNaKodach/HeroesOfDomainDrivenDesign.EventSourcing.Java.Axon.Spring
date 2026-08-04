package com.dddheroes.heroesofddd.tracing;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures.BuiltDwellingReadModelRepository;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.utils.AggregateEventPublisher;
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
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end trace-shape test through the REAL Axon pipeline: an aggregate event is appended to the event store,
 * the subscribing event processors invoke the reactive {@code @EventHandler}s
 * ({@code WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor.on(DwellingBuilt)} and
 * {@code DwellingReadModelProjector}), whose Spring Data R2DBC repository calls execute real Postgres queries with
 * I/O completing on the driver's shared Netty event-loop threads.
 * <p>
 * Asserts the exact property that used to break in production traces: every R2DBC {@code query} span must join the
 * event handler's trace with a valid parent -- never appear as an orphaned root in its own trace. This exercises the
 * full propagation chain: Axon's span-scoped drain window → raw Micrometer span captured into the Reactor Context →
 * Axoniq Framework's balanced span accessor restoring it across nested reactive scopes → Spring Boot's
 * {@code r2dbc-proxy} observation resolving its parent from that context.
 */
@Import({TestcontainersConfiguration.class, R2dbcTracingEndToEndTest.InMemorySpanExporterConfiguration.class})
@SpringBootTest(properties = {
        "management.tracing.enabled=true",
        "management.tracing.sampling.probability=1.0",
        "management.observations.enable.r2dbc=true",
        "spring.reactor.context-propagation=auto",
        "jdbc.datasource-proxy.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class R2dbcTracingEndToEndTest {

    private final String GAME_ID = GameId.random().raw();
    private final String PLAYER_ID = PlayerId.random().raw();

    @Autowired
    private AggregateEventPublisher aggregateEventPublisher;

    @Autowired
    private BuiltDwellingReadModelRepository builtDwellingReadModelRepository;

    @Autowired
    private CollectingSpanExporter spanExporter;

    @BeforeEach
    void resetExporter() {
        spanExporter.spans.clear();
    }

    @Test
    void r2dbcQuerySpansOfReactiveEventHandlersJoinTheEventProcessorTraceInsteadOfOrphaning() {
        // given / when -- a DwellingBuilt event flows through the real pipeline into the reactive projectors
        var dwellingId = DwellingId.random();
        var event = DwellingBuilt.event(
                dwellingId, CreatureId.of("angel"), Resources.from(ResourceType.GOLD, Amount.of(1000)));
        aggregateEventPublisher.publish("Dwelling", dwellingId.raw(),
                                        GameMetaData.with(GAME_ID, PLAYER_ID), event);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(builtDwellingReadModelRepository.findById(dwellingId.raw()).blockOptional()).isPresent());

        // then -- each reactive handler's trace contains its own R2DBC query spans (SELECT + INSERT), correctly
        // parented, instead of the queries orphaning into their own root traces. (The test's Awaitility polling
        // above issues findById queries of its own on the test thread with no active trace -- those are correctly
        // rootless and deliberately not asserted on.)
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            List<SpanData> spans = List.copyOf(spanExporter.spans);
            assertHandlerTraceContainsParentedQuerySpans(spans,
                    "WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor.on(DwellingBuilt,String)");
            assertHandlerTraceContainsParentedQuerySpans(spans,
                    "DwellingReadModelProjector.on(DwellingBuilt,String,QueryUpdateEmitter)");
        });
    }

    private static void assertHandlerTraceContainsParentedQuerySpans(List<SpanData> spans, String handlerSpanName) {
        SpanData handlerSpan = spans.stream()
                .filter(s -> s.getName().equals(handlerSpanName))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "handler span '" + handlerSpanName + "' not exported yet; recorded: "
                                + spans.stream().map(SpanData::getName).distinct().toList()));
        List<SpanData> queriesInHandlerTrace = spans.stream()
                .filter(s -> s.getName().equals("query"))
                .filter(s -> s.getTraceId().equals(handlerSpan.getTraceId()))
                .toList();
        assertThat(queriesInHandlerTrace)
                .as("r2dbc query spans inside the trace of %s (findById + save)", handlerSpanName)
                .hasSizeGreaterThanOrEqualTo(2)
                .allSatisfy(query -> assertThat(query.getParentSpanContext().isValid()).isTrue());
    }

    /** Minimal in-memory {@link SpanExporter} so this test needs no extra test dependency. */
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
