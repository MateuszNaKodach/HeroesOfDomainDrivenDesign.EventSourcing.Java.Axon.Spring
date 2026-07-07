package com.dddheroes.heroesofddd.creaturerecruitment.read.listdwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("games/{gameId}")
class ListDwellingsRestApi {

    private final QueryGateway queryGateway;

    ListDwellingsRestApi(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping("/dwellings/list")
    CompletableFuture<List<DwellingReadModel>> listDwellings(@PathVariable String gameId) {
        var query = ListDwellings.query(gameId);

        return queryGateway.query(
                query,
                ResponseTypes.multipleInstancesOf(DwellingReadModel.class)
        );
    }
}
