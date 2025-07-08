package com.dddheroes.heroesofddd.creaturerecruitment.write;

import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingSnapshot;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventsourcing.AggregateSnapshotter;
import org.axonframework.eventsourcing.EventSourcedAggregate;
import org.axonframework.eventsourcing.eventstore.DomainEventStream;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.modelling.command.RepositoryProvider;

public class DwellingSnapshotter extends AggregateSnapshotter {
    public DwellingSnapshotter(EventStore eventStore, RepositoryProvider repositoryProvider) {
        super(eventStore, repositoryProvider);
    }

    @Override
    protected DomainEventMessage createSnapshot(Class<?> aggregateType, String aggregateIdentifier, DomainEventStream eventStream) {
        return super.createSnapshot(aggregateType, aggregateIdentifier, eventStream);
    }

    @Override
    protected Object createSnapshot(Class<?> aggregateType, String aggregateIdentifier, EventSourcedAggregate<?> aggregate) {
        if (aggregate.getAggregateRoot() instanceof Dwelling dwelling) {
            return new DwellingSnapshot(
                dwelling.dwellingId != null ? dwelling.dwellingId.toString() : null,
                dwelling.creatureId != null ? dwelling.creatureId.toString() : null,
                dwelling.costPerTroop,
                dwelling.availableCreatures != null ? dwelling.availableCreatures.raw() : 0
            );
        }
        return super.createSnapshot(aggregateType, aggregateIdentifier, aggregate);
    }
} 