package com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
class IncreaseAvailableCreaturesRestApi {

    record Body(String creatureId, Integer increaseBy) {

    }

    private final CommandGateway commandGateway;

    IncreaseAvailableCreaturesRestApi(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @PutMapping("/dwellings/{dwellingId}/available-creatures-increases")
    CompletableFuture<Void> putDwellingAvailableCreaturesIncreases(
            @PathVariable String dwellingId,
            @RequestBody Body requestBody
    ) {
        var command = IncreaseAvailableCreatures.command(
                dwellingId,
                requestBody.creatureId(),
                requestBody.increaseBy()
        );
        return commandGateway.send(command);
    }
}
