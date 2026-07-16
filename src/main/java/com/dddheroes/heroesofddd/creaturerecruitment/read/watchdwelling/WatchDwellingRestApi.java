package com.dddheroes.heroesofddd.creaturerecruitment.read.watchdwelling;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import org.axonframework.extension.reactor.messaging.queryhandling.gateway.ReactorQueryGateway;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Exposes the subscription query as Server-Sent Events: emits the dwelling's current state, then a new
 * event every time its available creatures change or creatures are recruited — until the client
 * disconnects. {@code ReactorQueryGateway.subscriptionQuery} returns a single {@link Flux} that
 * combines the initial result with the updates emitted by the projector.
 */
@RestController
@RequestMapping("games/{gameId}")
class WatchDwellingRestApi {

    private final ReactorQueryGateway queryGateway;

    WatchDwellingRestApi(ReactorQueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping(value = "/dwellings/{dwellingId}/watch", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<DwellingReadModel> watchDwelling(
            @PathVariable String gameId,
            @PathVariable String dwellingId
    ) {
        var query = WatchDwelling.query(gameId, dwellingId);

        return queryGateway.subscriptionQuery(query, DwellingReadModel.class);
    }
}
