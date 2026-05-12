package com.dddheroes.heroesofddd.creaturerecruitment.automation;

import com.dddheroes.heroesofddd.armies.write.addcreature.AddCreatureToArmy;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import org.axonframework.messaging.commandhandling.gateway.CommandDispatcher;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.annotation.MetadataValue;
import org.axonframework.messaging.core.annotation.Namespace;
import org.axonframework.messaging.core.annotation.SequencingPolicy;
import org.axonframework.messaging.core.sequencing.MetadataSequencingPolicy;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.eventhandling.replay.annotation.DisallowReplay;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Namespace("Automation_WhenCreatureRecruitedThenAddToArmy_Processor")
@SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = GameMetaData.GAME_ID_KEY)
@DisallowReplay
@Component
class WhenCreatureRecruitedThenAddToArmyProcessor {

    @EventHandler
    CompletableFuture<?> react(
            CreatureRecruited event,
            @MetadataValue(GameMetaData.GAME_ID_KEY) String gameId,
            @MetadataValue(GameMetaData.PLAYER_ID_KEY) String playerId, CommandDispatcher commandDispatcher) {
        var metadata = GameMetaData.with(gameId, playerId);
        var command = AddCreatureToArmy.command(
                event.toArmy(),
                event.creatureId(),
                event.quantity()
        );
        CompletableFuture<Message> result = commandDispatcher.send(command, metadata).getResultMessage()
                .thenApply(m -> m);
        return result.exceptionallyCompose(error -> {
            var compensatingAction = IncreaseAvailableCreatures.command(
                    event.dwellingId(),
                    event.creatureId(),
                    event.quantity()
            );
            return commandDispatcher.send(compensatingAction, metadata).getResultMessage().thenApply(m -> m);
        });
    }
}
