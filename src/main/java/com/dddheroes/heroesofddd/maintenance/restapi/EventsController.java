package com.dddheroes.heroesofddd.maintenance.restapi;

import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@ConditionalOnProperty(name = "application.maintenance.enabled", havingValue = "true")
@RestController
class EventsController {

    private final EventStorageEngine eventStorageEngine;

    EventsController(EventStorageEngine eventStorageEngine) {
        this.eventStorageEngine = eventStorageEngine;
    }

    @CrossOrigin
    @GetMapping("/maintenance/event-store/streams/{streamId}/events")
    List<? extends DomainEventMessage<?>> readEvents(@PathVariable("streamId") String streamId) {
        return eventStorageEngine.readEvents(streamId).asStream().toList();
    }
}
