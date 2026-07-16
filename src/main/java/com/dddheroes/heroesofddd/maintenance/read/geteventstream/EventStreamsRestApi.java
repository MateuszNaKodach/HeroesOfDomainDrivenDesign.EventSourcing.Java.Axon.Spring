package com.dddheroes.heroesofddd.maintenance.read.geteventstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.SourcingCondition;
import org.axonframework.eventsourcing.eventstore.TerminalEventMessage;
import org.axonframework.messaging.core.FluxUtils;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.conversion.EventConverter;
import org.axonframework.messaging.eventstreaming.EventCriteria;
import org.axonframework.messaging.eventstreaming.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@ConditionalOnProperty(name = "application.maintenance.enabled", havingValue = "true")
@RestController
class EventStreamsRestApi {

    record StoredEvent(String identifier, String name, JsonNode payload, Map<String, String> metadata,
                       Instant timestamp) {

    }

    private final EventStorageEngine eventStorageEngine;
    private final EventConverter eventConverter;
    private final ObjectMapper objectMapper;

    EventStreamsRestApi(EventStorageEngine eventStorageEngine, EventConverter eventConverter,
                        ObjectMapper objectMapper) {
        this.eventStorageEngine = eventStorageEngine;
        this.eventConverter = eventConverter;
        this.objectMapper = objectMapper;
    }

    @CrossOrigin
    @GetMapping("/maintenance/event-store/streams/{streamId}/events")
    Mono<List<StoredEvent>> readEvents(@PathVariable String streamId) {
        // Identifiers follow the "Type:raw" convention (see e.g. DwellingId): the part before the
        // colon is the tag key, the full identifier is the tag value.
        var tagKey = streamId.contains(":") ? streamId.substring(0, streamId.indexOf(':')) : streamId;
        var condition = SourcingCondition.conditionFor(EventCriteria.havingTags(Tag.of(tagKey, streamId)));
        // subscribeOn: the JPA storage engine loads events inline on the subscribing thread, which
        // must not be the WebFlux event loop. (No-op cost when running against Axon Server.)
        return Flux.defer(() -> FluxUtils.of(eventStorageEngine.source(condition)))
                   .map(MessageStream.Entry::message)
                   // The stream's last entry is a TerminalEventMessage (payload-less end-of-stream marker).
                   .filter(message -> !(message instanceof TerminalEventMessage))
                   .map(this::toStoredEvent)
                   .collectList()
                   .subscribeOn(Schedulers.boundedElastic());
    }

    private StoredEvent toStoredEvent(EventMessage message) {
        return new StoredEvent(
                message.identifier(),
                message.type().qualifiedName().toString(),
                readTree(message.payloadAs(byte[].class, eventConverter)),
                message.metadata(),
                message.timestamp()
        );
    }

    private JsonNode readTree(byte[] payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot parse event payload as JSON", e);
        }
    }
}
