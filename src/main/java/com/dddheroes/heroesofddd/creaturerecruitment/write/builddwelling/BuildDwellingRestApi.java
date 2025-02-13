package com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling;

import com.dddheroes.heroesofddd.shared.GameMetaData;
import com.dddheroes.heroesofddd.shared.restapi.Headers;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("games/{gameId}")
class BuildDwellingRestApi {

    record Body(String creatureId, Map<String, Integer> costPerTroop) {

    }

    private final CommandGateway commandGateway;

    BuildDwellingRestApi(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PutMapping("/dwellings/{dwellingId}")
    CompletableFuture<Void> putDwellings(
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
