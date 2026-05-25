package com.dddheroes.heroesofddd.shared.infrastructure;

import io.opentelemetry.api.OpenTelemetry;
import org.axonframework.tracing.opentelemetry.OpenTelemetrySpanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("observability")
@Configuration
class TracingConfiguration {

    @Bean
    OpenTelemetrySpanFactory openTelemetrySpanFactory(OpenTelemetry openTelemetry) {
        // Both tracer AND propagator must come from the same Micrometer-managed OpenTelemetry —
        // otherwise the Builder falls back to no-op GlobalOpenTelemetry.getPropagators() and
        // propagateContext() / createHandlerSpan() silently lose the W3C trace context across
        // async boundaries (event processors, deadlines, Axon Server gRPC).
        return OpenTelemetrySpanFactory.builder()
                .tracer(openTelemetry.getTracer("axon-framework"))
                .contextPropagators(openTelemetry.getPropagators().getTextMapPropagator())
                .build();
    }
}
