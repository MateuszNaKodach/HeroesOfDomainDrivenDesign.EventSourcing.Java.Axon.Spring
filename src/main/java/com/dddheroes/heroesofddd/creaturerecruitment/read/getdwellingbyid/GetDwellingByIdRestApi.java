package com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("games/{gameId}")
class GetDwellingByIdRestApi {

    private final QueryGateway queryGateway;

    GetDwellingByIdRestApi(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @GetMapping("/dwellings/{dwellingId}")
    CompletableFuture<DwellingReadModel> getDwellings(
            @PathVariable String gameId,
            @PathVariable String dwellingId
    ) {
        var query = GetDwellingById.query(dwellingId, gameId);

        return queryGateway.query(
                query,
                DwellingReadModel.class
        );
    }
}
