package com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures;

import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.application.ReactiveGameCommandGateway;
import com.dddheroes.heroesofddd.shared.restapi.Headers;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/games/{gameId}")
class IncreaseAvailableCreaturesRestApi {

    record Body(String creatureId, Integer increaseBy) {

    }

    private final ReactiveGameCommandGateway commandGateway;

    IncreaseAvailableCreaturesRestApi(ReactiveGameCommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PutMapping("/dwellings/{dwellingId}/available-creatures-increases")
    Mono<Void> putDwellingAvailableCreaturesIncreases(
            @RequestHeader(Headers.PLAYER_ID) String playerId,
            @PathVariable String gameId,
            @PathVariable String dwellingId,
            @RequestBody Body requestBody
    ) {
        var command = IncreaseAvailableCreatures.command(
                dwellingId,
                requestBody.creatureId(),
                requestBody.increaseBy()
        );
        return commandGateway.send(command, GameMetaData.with(gameId, playerId));
    }
}
