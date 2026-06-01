package com.dddheroes.heroesofddd;

import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import io.axoniq.framework.axonserver.connector.api.AxonServerConnectionManager;
import io.axoniq.framework.axonserver.connector.event.AggregateBasedAxonServerEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.snapshot.inmemory.InMemorySnapshotStore;
import org.axonframework.eventsourcing.snapshot.store.SnapshotStore;
import org.axonframework.messaging.core.correlation.CorrelationDataProvider;
import org.axonframework.messaging.core.correlation.MessageOriginProvider;
import org.axonframework.messaging.core.correlation.SimpleCorrelationDataProvider;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameConfiguration {

    @Bean
    public CorrelationDataProvider gameDataProvider() {
        return new SimpleCorrelationDataProvider(GameMetaData.GAME_ID_KEY, GameMetaData.PLAYER_ID_KEY);
    }

    @Bean
    public CorrelationDataProvider messageOriginProvider() {
        return new MessageOriginProvider();
    }

    /**
     * Snapshot store for the JPA/PostgreSQL event store path.
     * <p>
     * Snapshotting (declared per entity via {@code @Snapshotting}) only activates when a
     * {@link SnapshotStore} component is present; Spring Boot does not auto-provide one. AF5 core ships
     * only {@link InMemorySnapshotStore}, so snapshots are not persisted across restarts.
     * <p>
     * Only active when Axon Server is disabled. When Axon Server is enabled, it provides its own native
     * snapshot handling, so this in-memory store must not be registered.
     */
    @ConditionalOnProperty(prefix = "axon.axonserver", name = "enabled", havingValue = "false", matchIfMissing = true)
    @Bean
    public SnapshotStore snapshotStore() {
        return new InMemorySnapshotStore();
    }

    /**
     * Event store mode override, active only when Axon Server is enabled.
     * <p>
     * The class-level condition ({@code axon.axonserver.enabled=true}) and the bean-level condition
     * ({@code application.eventstore.mode=aggregate-based}) must BOTH hold for the override to apply —
     * {@code @ConditionalOnProperty} is not repeatable, so the two properties are split across the
     * nested configuration and the bean method.
     */
    @Configuration
    @ConditionalOnProperty(prefix = "axon.axonserver", name = "enabled", havingValue = "true")
    static class AxonServerEventStoreConfiguration {

        /**
         * Overrides the default Axon Server event storage engine with the aggregate-based one.
         * <p>
         * When connecting to Axon Server, Axon's autoconfiguration provides a DCB-based
         * (Dynamic Consistency Boundary) {@code EventStorageEngine} by default. This project still
         * models its write side as classic aggregates, so when {@code application.eventstore.mode} is
         * {@code aggregate-based} we explicitly register
         * {@link AggregateBasedAxonServerEventStorageEngine} instead.
         * <p>
         * Set {@code application.eventstore.mode=dcb} to skip this bean and keep Axon Server's default
         * DCB-based engine. Defaults to {@code aggregate-based} when the property is absent.
         */
        @ConditionalOnProperty(prefix = "application.eventstore", name = "mode", havingValue = "aggregate-based", matchIfMissing = true)
        @Bean
        public EventStorageEngine storageEngine(
                AxonServerConnectionManager connectionManager,
                EventConverter eventConverter
        ) {
            return new AggregateBasedAxonServerEventStorageEngine(
                    connectionManager.getConnection(),
                    eventConverter
            );
        }
    }
}
