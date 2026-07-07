package com.dddheroes.heroesofddd.creaturerecruitment.read.watchdwelling;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Exposes the subscription query as Server-Sent Events: emits the dwelling's current state, then a new
 * event every time its available creatures change or creatures are recruited — until the client
 * disconnects. Spring MVC adapts the returned reactive {@link Flux} to SSE (Project Reactor on classpath).
 */
@RestController
@RequestMapping("games/{gameId}")
class WatchDwellingRestApi {

    private final QueryGateway queryGateway;

    WatchDwellingRestApi(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping(value = "/dwellings/{dwellingId}/watch", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<DwellingReadModel> watchDwelling(
            @PathVariable String gameId,
            @PathVariable String dwellingId
    ) {
        var query = WatchDwelling.query(gameId, dwellingId);

        SubscriptionQueryResult<DwellingReadModel, DwellingReadModel> result = queryGateway.subscriptionQuery(
                query,
                ResponseTypes.instanceOf(DwellingReadModel.class),
                ResponseTypes.instanceOf(DwellingReadModel.class)
        );

        // initial state first, then every emitted update; close the subscription when the client leaves
        return Flux.concat(result.initialResult(), result.updates())
                   .doFinally(signal -> result.close());
    }
}
