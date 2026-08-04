package com.dddheroes.heroesofddd.shared.infrastructure;

import io.micrometer.observation.ObservationPredicate;
import io.micrometer.tracing.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Filters out the noisy standalone JDBC traces that datasource-micrometer produces on background threads which carry
 * no trace context — chiefly the pooled event processors' token-store claim/extend/release queries and HikariCP pool
 * warm-up. Those run on Axon coordinator/worker threads outside any request or handler segment, so each JDBC
 * observation ({@code jdbc.connection}, {@code jdbc.query}, {@code jdbc.result-set}) starts its own trace and shows up
 * as a standalone {@code connection} root in Jaeger.
 * <p>
 * datasource-micrometer cannot filter by the calling package/class (the observation is created in the JDBC proxy
 * layer, blind to the caller), and {@code jdbc.excluded-data-source-bean-names} is too coarse here (one shared
 * DataSource backs both the token store and the read-model projections). Instead this registers a Micrometer
 * {@link ObservationPredicate} that vetoes a {@code jdbc.*} observation only when it starts with <em>no current
 * span</em> — i.e. on a background thread with no propagated trace context. JDBC observations that occur inside a
 * request (on the request thread) or inside a framework handler segment (where the tracing context-propagation bridge
 * has made the handler/commit span current) keep a non-null current span and are retained, so their
 * connection/query/result-set spans still nest under the Axon/HTTP spans.
 * <p>
 * Spring Boot's observation auto-configuration applies every {@link ObservationPredicate} bean to the
 * {@code ObservationRegistry}. Gated to the {@code observability} profile and to datasource-proxy being enabled and on
 * the classpath, so lean builds skip it.
 */
@Configuration(proxyBeanMethods = false)
@Profile("observability")
@ConditionalOnClass(name = "net.ttddyy.observation.tracing.DataSourceObservationListener")
@ConditionalOnProperty(prefix = "jdbc.datasource-proxy", name = "enabled", havingValue = "true")
public class JdbcBackgroundConnectionTracingFilterConfiguration {

    private static final String JDBC_OBSERVATION_PREFIX = "jdbc.";

    /**
     * Retains a {@code jdbc.*} observation only when a trace is already active on the current thread; drops the
     * parentless background ones (token store, pool warm-up) that would otherwise become standalone root traces.
     *
     * @param tracer the Micrometer tracer used to detect whether a span is current on the calling thread
     * @return an {@link ObservationPredicate} vetoing parentless JDBC observations
     */
    @Bean
    ObservationPredicate skipParentlessJdbcObservations(Tracer tracer) {
        return (name, context) -> {
            if (name != null && name.startsWith(JDBC_OBSERVATION_PREFIX)) {
                return tracer.currentSpan() != null;
            }
            return true;
        };
    }
}
