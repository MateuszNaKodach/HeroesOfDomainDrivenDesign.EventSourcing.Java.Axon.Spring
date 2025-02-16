package com.dddheroes.heroesofddd.maintenance.read.geteventstream;

import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ConditionalOnProperty(name = "application.maintenance.enabled", havingValue = "true")
@RestController
class EventStreamsRestApi {

    private final EventStore eventStore;

    EventStreamsRestApi(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @CrossOrigin
    @GetMapping("/maintenance/event-store/streams/{streamId}/events")
    List<? extends DomainEventMessage<?>> readEvents(@PathVariable String streamId) {
        return eventStore.readEvents(streamId).asStream().toList();
    }
}
