package com.dddheroes.heroesofddd.utils;

import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@Component
public class EventStoreAssertions {

    private final EventStore eventStore;

    EventStoreAssertions(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    void assertEvent(String aggregateId, Predicate<? super Object> predicate) {
        assertThat(eventStore.readEvents(aggregateId).asStream().map(Message::getPayload).toList())
                .anyMatch(predicate);
    }
}
