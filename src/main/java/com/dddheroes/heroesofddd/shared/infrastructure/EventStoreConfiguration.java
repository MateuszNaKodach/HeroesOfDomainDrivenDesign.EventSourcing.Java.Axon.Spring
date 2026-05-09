package com.dddheroes.heroesofddd.shared.infrastructure;

import jakarta.persistence.EntityManagerFactory;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.AggregateBasedJpaEventStorageEngine;
import org.axonframework.messaging.core.unitofwork.transaction.jpa.JpaTransactionalExecutorProvider;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.UnaryOperator;

// Forces aggregate-based JPA storage. Without this @Bean, axon-server-connector's
// ServiceLoader-discovered AxonServerConfigurationEnhancer (order = MIN_VALUE+10)
// runs before JpaEventStoreAutoConfiguration's enhancer (order ≈ MAX_VALUE) and
// registers AxonServerEventStorageEngine (DCB-flat) via registerIfNotPresent —
// even with axon.axonserver.enabled=false. SearchScope.ALL on registerIfNotPresent
// makes a Spring bean of EventStorageEngine win.
@Configuration
public class EventStoreConfiguration {

    @Bean
    public EventStorageEngine eventStorageEngine(EntityManagerFactory entityManagerFactory,
                                                 EventConverter eventConverter) {
        return new AggregateBasedJpaEventStorageEngine(
                new JpaTransactionalExecutorProvider(entityManagerFactory),
                eventConverter,
                UnaryOperator.identity()
        );
    }
}
