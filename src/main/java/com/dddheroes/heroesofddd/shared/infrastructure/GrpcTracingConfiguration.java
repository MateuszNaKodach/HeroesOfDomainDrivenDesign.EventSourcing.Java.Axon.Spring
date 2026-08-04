package com.dddheroes.heroesofddd.shared.infrastructure;

import io.grpc.ClientInterceptor;
import io.opentelemetry.api.OpenTelemetry;
import org.axonframework.axonserver.connector.ManagedChannelCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
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
 * <b>Build-time gate.</b> The {@code opentelemetry-grpc-1.6} instrumentation JAR that supplies
 * {@code io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry} is opt-in: it is only added to the artifact
 * when the app is built with {@code -Dtracing.grpc.enabled=true} (the {@code tracing-grpc} Maven profile). To keep
 * this class compilable and loadable when that JAR is absent, {@code GrpcTelemetry} is <b>not</b> imported at
 * compile time - it is resolved reflectively - and the whole configuration is {@code @ConditionalOnClass} on it,
 * so Spring skips it entirely on a lean build. Note {@link ClientInterceptor} (io.grpc) and {@link OpenTelemetry}
 * (io.opentelemetry.api) are always on the classpath - the former via axon-server-connector, the latter via the
 * always-present micrometer/OTLP tracing bridge - so only {@code GrpcTelemetry} needs the reflective path.
 * <p>
 * <b>Runtime gates.</b> Beyond the JAR being present, activation still requires:
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
@ConditionalOnClass(name = "io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry")
@ConditionalOnProperty(prefix = "axon.axonserver", name = "enabled", havingValue = "true")
public class GrpcTracingConfiguration {

    @Bean
    public ManagedChannelCustomizer tracingManagedChannelCustomizer(OpenTelemetry openTelemetry) throws Exception {
        // GrpcTelemetry lives in the opt-in opentelemetry-grpc-1.6 JAR (see class Javadoc), so it is resolved
        // reflectively rather than imported. @ConditionalOnClass above guarantees the class is present here.
        Class<?> grpcTelemetry = Class.forName("io.opentelemetry.instrumentation.grpc.v1_6.GrpcTelemetry");
        Object telemetry = grpcTelemetry.getMethod("create", OpenTelemetry.class).invoke(null, openTelemetry);
        ClientInterceptor interceptor =
                (ClientInterceptor) grpcTelemetry.getMethod("newClientInterceptor").invoke(telemetry);
        return builder -> builder.intercept(interceptor);
    }
}
