package com.dddheroes.heroesofddd.utils;

import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.Message;
import org.springframework.stereotype.Component;

import static org.junit.jupiter.api.Assertions.*;

@Component
public class EventStoreAssertions {

    private final EventStore eventStore;

    EventStoreAssertions(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    public void assertEventStored(String streamId, Class<?> eventType) {
        var events = eventStore.readEvents(streamId).asStream().map(e -> e.getPayload().getClass());
        assertTrue(events.anyMatch(eventType::equals));
    }

    public void assertEventNotStored(String streamId, Class<?> eventType) {
        var events = eventStore.readEvents(streamId).asStream().map(e -> e.getPayload().getClass());
        assertTrue(events.noneMatch(eventType::equals));
    }

    public void assertEventStored(String streamId, Object payload) {
        var events = eventStore.readEvents(streamId).asStream().map(Message::getPayload);
        assertTrue(events.anyMatch(payload::equals));
    }

    public void assertNoEventsStored(String streamId) {
        assertEventsStoredCount(streamId, 0);
    }

    public void assertEventsStoredCount(String streamId, int count) {
        var events = eventStore.readEvents(streamId).asStream().count();
        assertEquals(count, events);
    }
}
