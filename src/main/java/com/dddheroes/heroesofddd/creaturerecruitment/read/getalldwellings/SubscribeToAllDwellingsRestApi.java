package com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings;

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

import java.time.Duration;

@RestController
@RequestMapping("games/{gameId}")
class SubscribeToAllDwellingsRestApi {

    private final QueryGateway queryGateway;

    SubscribeToAllDwellingsRestApi(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping(value = "/dwellings/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<DwellingReadModel> subscribeToAllDwellings(@PathVariable String gameId) {
        var query = GetAllDwellings.query(gameId);

        SubscriptionQueryResult<GetAllDwellings.Result, DwellingReadModel> subscriptionQuery =
                queryGateway.subscriptionQuery(
                        query,
                        ResponseTypes.instanceOf(GetAllDwellings.Result.class),
                        ResponseTypes.instanceOf(DwellingReadModel.class)
                );

        return subscriptionQuery
                .initialResult()
                .flatMapMany(result -> Flux.fromIterable(result.dwellings()))
                .concatWith(subscriptionQuery.updates())
                .doFinally(signal -> subscriptionQuery.close())
                .timeout(Duration.ofMinutes(30)); // Close subscription after 30 minutes of inactivity
    }
}