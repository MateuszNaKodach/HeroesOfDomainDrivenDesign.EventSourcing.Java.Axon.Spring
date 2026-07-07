package com.dddheroes.heroesofddd.shared.infrastructure;

import io.grpc.ClientInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry;
import org.axonframework.axonserver.connector.ManagedChannelCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * gRPC tracing for the Axon Server connector channel.
 * <p>
 * The only gRPC in this app is the connection to Axon Server (axon-server-connector). This registers an
 * OpenTelemetry {@link ClientInterceptor} on that channel via Axon 4's {@link ManagedChannelCustomizer} hook:
 * {@code AxonServerAutoConfiguration} wires the {@code ManagedChannelCustomizer} bean into the
 * {@code AxonServerConnectionManager} channel builder, and its own default customizer is
 * {@code @ConditionalOnMissingBean} identity, so this Spring bean wins. The interceptor is built from the same
 * {@link OpenTelemetry} SDK bean as the Axon/HTTP/JDBC spans, so gRPC client spans (rpc.system=grpc,
 * rpc.service/rpc.method, status) flow through the existing OTLP pipeline.
 * <p>
 * Gated to both conditions so it neither requires the (profile-only) {@link OpenTelemetry} bean nor does any work
 * when there is no channel to instrument:
 * <ul>
 *     <li>{@code @Profile("observability")} - the {@link OpenTelemetry} bean only exists when tracing is on
 *     (the {@code observability-jaeger}/{@code observability-elastic} groups include {@code observability}).</li>
 *     <li>{@code axon.axonserver.enabled=true} - otherwise the app runs on the JPA event store with no gRPC.</li>
 * </ul>
 * <p>
 * Note: Axon Server traffic is mostly long-lived bidirectional streams (command/query/event/control channels).
 * gRPC client instrumentation opens one span per RPC, so a streaming call yields a single span spanning the whole
 * stream lifetime rather than one per message - useful for connection/stream lifecycle and errors. Per-message
 * command/event/query tracing is already provided by the framework's distributed tracing (axon-tracing-opentelemetry).
 */
@Configuration(proxyBeanMethods = false)
@Profile("observability")
@ConditionalOnProperty(prefix = "axon.axonserver", name = "enabled", havingValue = "true")
public class GrpcTracingConfiguration {

    @Bean
    public ManagedChannelCustomizer tracingManagedChannelCustomizer(OpenTelemetry openTelemetry) {
        ClientInterceptor interceptor = GrpcTelemetry.create(openTelemetry).newClientInterceptor();
        return builder -> builder.intercept(interceptor);
    }
}
