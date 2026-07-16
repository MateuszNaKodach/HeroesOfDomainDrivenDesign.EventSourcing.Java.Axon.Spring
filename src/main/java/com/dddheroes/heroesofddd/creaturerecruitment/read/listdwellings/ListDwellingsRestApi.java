package com.dddheroes.heroesofddd.creaturerecruitment.read.listdwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import org.axonframework.extension.reactor.messaging.queryhandling.gateway.ReactorQueryGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("games/{gameId}")
class ListDwellingsRestApi {

    private final ReactorQueryGateway queryGateway;

    ListDwellingsRestApi(ReactorQueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping("/dwellings/list")
    Mono<List<DwellingReadModel>> listDwellings(@PathVariable String gameId) {
        var query = ListDwellings.query(gameId);

        return queryGateway.queryMany(
                query,
                DwellingReadModel.class
        );
    }
}
