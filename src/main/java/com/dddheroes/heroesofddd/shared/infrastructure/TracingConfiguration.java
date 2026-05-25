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
        return OpenTelemetrySpanFactory.builder()
                .tracer(openTelemetry.getTracer("axon-framework"))
                .build();
    }
}
