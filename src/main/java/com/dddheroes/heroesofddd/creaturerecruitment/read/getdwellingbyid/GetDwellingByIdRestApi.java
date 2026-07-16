package com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import org.axonframework.extension.reactor.messaging.queryhandling.gateway.ReactorQueryGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("games/{gameId}")
class GetDwellingByIdRestApi {

    private final ReactorQueryGateway queryGateway;

    GetDwellingByIdRestApi(ReactorQueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping("/dwellings/{dwellingId}")
    Mono<DwellingReadModel> getDwellings(
            @PathVariable String gameId,
            @PathVariable String dwellingId
    ) {
        var query = GetDwellingById.query(gameId, dwellingId);

        return queryGateway.query(
                query,
                DwellingReadModel.class
        );
    }
}
