package com.dddheroes.heroesofddd.creaturerecruitment.read.streamdwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import org.axonframework.extension.reactor.messaging.queryhandling.gateway.ReactorQueryGateway;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Exposes the streaming query as Server-Sent Events. The whole pipeline is reactive: the handler
 * streams rows lazily from the R2DBC repository and {@code ReactorQueryGateway.streamingQuery}
 * returns a native {@link Flux}.
 */
@RestController
@RequestMapping("games/{gameId}")
class StreamDwellingsRestApi {

    private final ReactorQueryGateway queryGateway;

    StreamDwellingsRestApi(ReactorQueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping(value = "/dwellings/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<DwellingReadModel> streamDwellings(@PathVariable String gameId) {
        var query = StreamDwellings.query(gameId);

        return queryGateway.streamingQuery(query, DwellingReadModel.class);
    }
}
