package com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures;

import com.dddheroes.heroesofddd.shared.GameMetaData;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.unitofwork.annotation.ProcessingContext;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/games/{gameId}")
class IncreaseAvailableCreaturesRestApi {

    record Body(String creatureId, Integer increaseBy) {

    }

    private final CommandGateway commandGateway;

    IncreaseAvailableCreaturesRestApi(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PutMapping("/dwellings/{dwellingId}/available-creatures-increases")
    CompletableFuture<Void> putDwellingAvailableCreaturesIncreases(
            @PathVariable String gameId,
            @PathVariable String dwellingId,
            @RequestBody Body requestBody,
            @ProcessingContext org.axonframework.messaging.unitofwork.ProcessingContext processingContext
    ) {
        var command = IncreaseAvailableCreatures.command(
                dwellingId,
                requestBody.creatureId(),
                requestBody.increaseBy()
        );
        return commandGateway.send(command, GameMetaData.withId(gameId), processingContext).resultAs(Void.class);
    }
}
