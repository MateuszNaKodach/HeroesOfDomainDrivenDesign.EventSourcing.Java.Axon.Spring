package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
class RecruitCreatureRestApi {

    record Body(String creatureId, String armyId, Integer quantity) {

    }

    private final CommandGateway commandGateway;

    RecruitCreatureRestApi(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PutMapping("/dwellings/{dwellingId}/creature-recruitments")
    CompletableFuture<Void> putDwellingsCreatureRecruitments(
            @PathVariable String dwellingId,
            @RequestBody Body requestBody
    ) {
        var command = RecruitCreature.command(
                dwellingId,
                requestBody.creatureId(),
                requestBody.armyId(),
                requestBody.quantity()
        );
        return commandGateway.send(command);
    }
}
