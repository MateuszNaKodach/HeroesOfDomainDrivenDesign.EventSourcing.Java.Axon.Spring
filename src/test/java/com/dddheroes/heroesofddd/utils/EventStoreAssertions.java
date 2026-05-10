package com.dddheroes.heroesofddd.utils;

import org.axonframework.eventsourcing.eventstore.ConsistencyMarker;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.SourcingCondition;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventstreaming.EventCriteria;
import org.axonframework.messaging.eventstreaming.Tag;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * AF4's eventStore.readEvents(streamId).asStream() is gone in AF5: EventStore is now an EventBus
 * + StreamableEventSource and exposes only transaction(ProcessingContext) for appending.
 * Aggregate-stream reads happen via EventStorageEngine.source(SourcingCondition) using a
 * tag-based EventCriteria — Tag(key=aggregateType, value=aggregateId) per the aggregate's
 * @EventTag declaration. We read the resulting MessageStream synchronously into a List for assertions.
 */
@Component
public class EventStoreAssertions {

    private final EventStorageEngine eventStorageEngine;

    EventStoreAssertions(EventStorageEngine eventStorageEngine) {
        this.eventStorageEngine = eventStorageEngine;
    }

    public void assertEventStored(String aggregateType, String aggregateId, Class<?> eventType) {
        var events = readEvents(aggregateType, aggregateId);
        assertTrue(events.stream().map(e -> e.payload().getClass()).anyMatch(eventType::equals),
                () -> "Expected event of type " + eventType.getName() + " for "
                      + aggregateType + " " + aggregateId + " but found: " + payloadTypes(events));
    }

    public void assertEventNotStored(String aggregateType, String aggregateId, Class<?> eventType) {
        var events = readEvents(aggregateType, aggregateId);
        assertTrue(events.stream().map(e -> e.payload().getClass()).noneMatch(eventType::equals),
                () -> "Expected no event of type " + eventType.getName() + " for "
                      + aggregateType + " " + aggregateId + " but found one in: " + payloadTypes(events));
    }

    public void assertEventStored(String aggregateType, String aggregateId, Object payload) {
        var events = readEvents(aggregateType, aggregateId);
        assertTrue(events.stream().map(EventMessage::payload).anyMatch(payload::equals));
    }

    public void assertNoEventsStored(String aggregateType, String aggregateId) {
        assertEventsStoredCount(aggregateType, aggregateId, 0);
    }

    public void assertEventsStoredCount(String aggregateType, String aggregateId, int count) {
        assertEquals(count, readEvents(aggregateType, aggregateId).size());
    }

    private List<EventMessage> readEvents(String aggregateType, String aggregateId) {
        var condition = SourcingCondition.conditionFor(
                EventCriteria.havingTags(Tag.of(aggregateType, aggregateId))
        );
        MessageStream<EventMessage> stream = eventStorageEngine.source(condition);
        // AggregateBasedJpaEventStorageEngine appends a TerminalEventMessage at the end of the
        // sourced stream that carries only the ConsistencyMarker — filter those out by checking
        // the entry's ConsistencyMarker resource (real events don't carry it on their entry context).
        return stream.reduce(
                new ArrayList<EventMessage>(),
                (acc, entry) -> {
                    if (entry.getResource(ConsistencyMarker.RESOURCE_KEY) == null) {
                        acc.add(entry.message());
                    }
                    return acc;
                }
        ).join();
    }

    private static List<String> payloadTypes(List<EventMessage> events) {
        return events.stream().map(e -> e.payload().getClass().getSimpleName()).toList();
    }
}
