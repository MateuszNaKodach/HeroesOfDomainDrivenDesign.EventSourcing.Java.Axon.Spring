package com.dddheroes.heroesofddd.maintenance.read.geteventstream;

import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.SourcingCondition;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventstreaming.EventCriteria;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@ConditionalOnProperty(name = "application.maintenance.enabled", havingValue = "true")
@RestController
class EventStreamsRestApi {

    private final EventStorageEngine eventStorageEngine;

    EventStreamsRestApi(EventStorageEngine eventStorageEngine) {
        this.eventStorageEngine = eventStorageEngine;
    }

    @CrossOrigin
    @GetMapping("/maintenance/event-store/streams/{streamId}/events")
    List<EventMessage> readEvents(@PathVariable String streamId) {
        var condition = SourcingCondition.conditionFor(EventCriteria.havingTags(streamId));
        var stream = eventStorageEngine.source(condition);
        var events = new ArrayList<EventMessage>();
        while (stream.hasNextAvailable()) {
            stream.next().map(MessageStream.Entry::message).ifPresent(events::add);
        }
        return events;
    }
}
