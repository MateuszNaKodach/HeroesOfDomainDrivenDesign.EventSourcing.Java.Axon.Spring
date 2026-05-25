package com.dddheroes.heroesofddd.shared.infrastructure;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericEventMessage;
import org.axonframework.tracing.opentelemetry.OpenTelemetrySpanFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link TracingConfiguration} wires {@link OpenTelemetrySpanFactory} with the
 * Spring-managed {@link OpenTelemetry} (and NOT the no-op {@code GlobalOpenTelemetry}).
 * <p>
 * Without an explicit {@code contextPropagators(...)} the builder falls back to
 * {@code GlobalOpenTelemetry.getPropagators()}, which in a Spring Boot 3 + Micrometer Tracing
 * setup is a no-op — silently breaking W3C trace context propagation across Axon async boundaries
 * (event processors, deadlines, Axon Server gRPC). See {@code OpenTelemetryAutoConfiguration}
 * in axon-spring-boot-autoconfigure (4.13.x) for the same upstream bug.
 */
@DisplayName("TracingConfiguration")
class TracingConfigurationTest {

    @Nested
    @DisplayName("propagateContext via the configured OpenTelemetrySpanFactory")
    class PropagateContext {

        @Test
        @DisplayName("injects W3C traceparent into message metadata when configured with a Spring-managed OpenTelemetry")
        void shouldInjectTraceparentWhenConfiguredCorrectly() {
            // given — Spring-managed OpenTelemetry with a real W3C propagator
            OpenTelemetry openTelemetry = realOpenTelemetry();
            OpenTelemetrySpanFactory spanFactory = new TracingConfiguration().openTelemetrySpanFactory(openTelemetry);

            // when — propagate the current span context into an Axon message
            Tracer tracer = openTelemetry.getTracer("test");
            Span currentSpan = tracer.spanBuilder("root").startSpan();
            EventMessage<String> propagated;
            try (Scope ignored = currentSpan.makeCurrent()) {
                propagated = spanFactory.propagateContext(GenericEventMessage.asEventMessage("payload"));
            } finally {
                currentSpan.end();
            }

            // then — the W3C traceparent header is present in the message metadata
            assertThat(propagated.getMetaData())
                    .as("W3C trace context must be injected into message metadata so async handlers can extract it")
                    .containsKey("traceparent");
        }

        @Test
        @DisplayName("regression guard: factory built WITHOUT explicit propagator (the upstream Axon bug) silently drops the context")
        void shouldDocumentTheBugWhenPropagatorIsNotConfigured() {
            // given — the buggy default: OpenTelemetrySpanFactory.builder().build() without
            // passing the Spring-managed OpenTelemetry. This is what
            // axon-spring-boot-autoconfigure's OpenTelemetryAutoConfiguration does today.
            OpenTelemetry openTelemetry = realOpenTelemetry();
            OpenTelemetrySpanFactory buggyFactory = OpenTelemetrySpanFactory.builder().build();

            // when — propagate the current span context into an Axon message
            Tracer tracer = openTelemetry.getTracer("test");
            Span currentSpan = tracer.spanBuilder("root").startSpan();
            EventMessage<String> propagated;
            try (Scope ignored = currentSpan.makeCurrent()) {
                propagated = buggyFactory.propagateContext(GenericEventMessage.asEventMessage("payload"));
            } finally {
                currentSpan.end();
            }

            // then — no traceparent injected: the factory used the no-op GlobalOpenTelemetry
            // propagator instead of the Spring-managed one. If this assertion ever starts
            // FAILING upstream, it means Axon fixed the bug and our explicit wiring becomes
            // optional (still harmless).
            assertThat(propagated.getMetaData())
                    .as("If this test breaks, the upstream Axon bug is fixed — our workaround can be simplified")
                    .doesNotContainKey("traceparent");
        }
    }

    private static OpenTelemetry realOpenTelemetry() {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder().build())
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
    }
}
