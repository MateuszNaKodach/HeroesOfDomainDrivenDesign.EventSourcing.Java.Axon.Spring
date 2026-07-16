package com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling;

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

import java.util.Map;

@RestController
@RequestMapping("games/{gameId}")
class BuildDwellingRestApi {

    record Body(String creatureId, Map<String, Integer> costPerTroop) {

    }

    private final ReactiveGameCommandGateway commandGateway;

    BuildDwellingRestApi(ReactiveGameCommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PutMapping("/dwellings/{dwellingId}")
    Mono<Void> putDwellings(
            @RequestHeader(Headers.PLAYER_ID) String playerId,
            @PathVariable String gameId,
            @PathVariable String dwellingId,
            @RequestBody Body requestBody
    ) {
        var command = BuildDwelling.command(
                dwellingId,
                requestBody.creatureId(),
                requestBody.costPerTroop()
        );
        return commandGateway.send(command, GameMetaData.with(gameId, playerId));
    }
}
