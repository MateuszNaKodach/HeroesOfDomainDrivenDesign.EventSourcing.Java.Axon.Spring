package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.shared.application.GameMetaData;
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
@RequestMapping("/games/{gameId}")
class RecruitCreatureRestApi {

    record Body(String creatureId, String armyId, Integer quantity, Map<String, Integer> expectedCost) {

    }

    private final CommandGateway commandGateway;

    RecruitCreatureRestApi(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PutMapping("/dwellings/{dwellingId}/creature-recruitments")
    CompletableFuture<Void> putDwellingsCreatureRecruitments(
            @RequestHeader(Headers.PLAYER_ID) String playerId,
            @PathVariable String gameId,
            @PathVariable String dwellingId,
            @RequestBody Body requestBody
    ) {
        var command = RecruitCreature.command(
                dwellingId,
                requestBody.creatureId(),
                requestBody.armyId(),
                requestBody.quantity(),
                requestBody.expectedCost()
        );
        return commandGateway.send(command, GameMetaData.with(gameId, playerId));
    }
}
