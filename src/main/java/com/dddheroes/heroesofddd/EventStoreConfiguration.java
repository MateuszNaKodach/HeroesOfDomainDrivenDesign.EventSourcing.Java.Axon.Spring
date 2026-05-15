package com.dddheroes.heroesofddd;

import jakarta.persistence.EntityManagerFactory;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.AggregateBasedJpaEventStorageEngine;
import org.axonframework.messaging.core.unitofwork.transaction.jpa.JpaTransactionalExecutorProvider;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.UnaryOperator;

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
