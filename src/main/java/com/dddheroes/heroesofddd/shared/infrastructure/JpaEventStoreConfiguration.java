package com.dddheroes.heroesofddd.shared.infrastructure;

import jakarta.persistence.EntityManagerFactory;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.SnapshotCapableEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.AggregateBasedJpaEventStorageEngine;
import org.axonframework.eventsourcing.snapshot.inmemory.InMemorySnapshotStore;
import org.axonframework.eventsourcing.snapshot.store.SnapshotStore;
import org.axonframework.messaging.core.unitofwork.transaction.jpa.JpaTransactionalExecutorProvider;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.UnaryOperator;

/**
 * Event store configuration for the JPA/PostgreSQL path, active when Axon Server is disabled.
 *
 * @see AxonServerEventStoreConfiguration
 */
@Configuration
@EntityScan(basePackages = {
        "com.dddheroes.heroesofddd",
        "org.axonframework",
        "io.axoniq.framework"
})
@ConditionalOnProperty(name = "axon.axonserver.enabled", havingValue = "false", matchIfMissing = true)
public class JpaEventStoreConfiguration {

    /**
     * Snapshot store for the JPA/PostgreSQL event store path.
     * <p>
     * Snapshotting (declared per entity via {@code @Snapshotting}) only activates when a {@link SnapshotStore}
     * component is present; Spring Boot does not auto-provide one. AF5 core ships only
     * {@link InMemorySnapshotStore}, so snapshots are not persisted across restarts.
     */
    @Bean
    public SnapshotStore snapshotStore() {
        return new InMemorySnapshotStore();
    }

    @Bean
    public EventStorageEngine eventStorageEngine(EntityManagerFactory entityManagerFactory,
                                                 EventConverter eventConverter,
                                                 SnapshotStore snapshotStore) {
        // The JPA engine has no native snapshot support. Overriding the EventStorageEngine as a Spring bean
        // bypasses the SnapshotCapableEventStorageEngine decorator that AF5 would otherwise apply, so we wrap
        // it explicitly here. Without this, sourcing a @Snapshotting entity (Snapshot strategy) fails with
        // "Unsupported sourcing strategy: Snapshot[...]".
        return new SnapshotCapableEventStorageEngine(
                new AggregateBasedJpaEventStorageEngine(
                        new JpaTransactionalExecutorProvider(entityManagerFactory),
                        eventConverter,
                        UnaryOperator.identity()
                ),
                snapshotStore
        );
    }
}
