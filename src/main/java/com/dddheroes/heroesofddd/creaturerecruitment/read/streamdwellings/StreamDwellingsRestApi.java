package com.dddheroes.heroesofddd.creaturerecruitment.read.streamdwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Exposes the streaming query as Server-Sent Events. Spring MVC adapts the returned reactive
 * {@link Flux} to an SSE response (no WebFlux required, since Project Reactor is on the classpath).
 */
@RestController
@RequestMapping("games/{gameId}")
class StreamDwellingsRestApi {

    private final QueryGateway queryGateway;

    StreamDwellingsRestApi(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping(value = "/dwellings/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<DwellingReadModel> streamDwellings(@PathVariable String gameId) {
        var query = StreamDwellings.query(gameId);

        return Flux.from(queryGateway.streamingQuery(query, DwellingReadModel.class));
    }
}
