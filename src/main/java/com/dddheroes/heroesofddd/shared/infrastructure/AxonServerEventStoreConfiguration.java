package com.dddheroes.heroesofddd.shared.infrastructure;

import io.axoniq.framework.axonserver.connector.api.AxonServerConnectionManager;
import io.axoniq.framework.axonserver.connector.event.AggregateBasedAxonServerEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.SnapshotCapableEventStorageEngine;
import org.axonframework.eventsourcing.snapshot.inmemory.InMemorySnapshotStore;
import org.axonframework.eventsourcing.snapshot.store.SnapshotStore;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Event store configuration for the Axon Server path, active when Axon Server is enabled.
 * <p>
 * The class-level condition ({@code axon.axonserver.enabled=true}) and the bean-level condition
 * ({@code application.eventstore.mode=aggregate-based}) must BOTH hold for the overrides to apply -
 * {@code @ConditionalOnProperty} is not repeatable, so the two properties are split across the class and the
 * bean methods. Set {@code application.eventstore.mode=dcb} to skip the overrides and keep Axon Server's
 * default DCB-based engine and snapshot store.
 *
 * @see JpaEventStoreConfiguration
 */
@Configuration
@ConditionalOnProperty(prefix = "axon.axonserver", name = "enabled", havingValue = "true")
public class AxonServerEventStoreConfiguration {

    /**
     * Snapshot store for the aggregate-based Axon Server path.
     * <p>
     * The connector's default {@link SnapshotStore} ({@code AxonServerSnapshotStore}) writes via Axon Server's
     * DCB snapshot channel, which a classic/aggregate-based context does not provide (storing fails with
     * "No snapshot updates store found for context"). So in {@code aggregate-based} mode we register a
     * backend-neutral {@link InMemorySnapshotStore} instead — used both to store (entity) and load (engine
     * wrapper) snapshots. Note: not persisted across restarts; swap for a persistent store if needed.
     */
    @ConditionalOnProperty(prefix = "application.eventstore", name = "mode", havingValue = "aggregate-based", matchIfMissing = true)
    @Bean
    public SnapshotStore snapshotStore() {
        return new InMemorySnapshotStore();
    }

    /**
     * Overrides the default Axon Server event storage engine with the aggregate-based one.
     * <p>
     * When connecting to Axon Server, Axon's autoconfiguration provides a DCB-based
     * (Dynamic Consistency Boundary) {@code EventStorageEngine} by default. This project still
     * models its write side as classic aggregates, so when {@code application.eventstore.mode} is
     * {@code aggregate-based} we explicitly register
     * {@link AggregateBasedAxonServerEventStorageEngine} instead.
     */
    @ConditionalOnProperty(prefix = "application.eventstore", name = "mode", havingValue = "aggregate-based", matchIfMissing = true)
    @Bean
    public EventStorageEngine storageEngine(
            AxonServerConnectionManager connectionManager,
            EventConverter eventConverter,
            SnapshotStore snapshotStore
    ) {
        // The aggregate-based engine has no native snapshot support. Overriding the EventStorageEngine as a Spring
        // bean bypasses the SnapshotCapableEventStorageEngine decorator that AF5 would otherwise apply, so we wrap
        // it explicitly here. Without this, sourcing a @Snapshotting entity (Snapshot strategy) reaches the raw
        // engine and fails with "No start position is available".
        return new SnapshotCapableEventStorageEngine(
                new AggregateBasedAxonServerEventStorageEngine(
                        connectionManager.getConnection(),
                        eventConverter
                ),
                snapshotStore
        );
    }
}
