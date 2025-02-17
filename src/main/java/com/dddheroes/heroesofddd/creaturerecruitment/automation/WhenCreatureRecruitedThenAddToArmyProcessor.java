package com.dddheroes.heroesofddd.creaturerecruitment.automation;

import com.dddheroes.heroesofddd.armies.write.addcreature.AddCreatureToArmy;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.CreatureRecruited;
import com.dddheroes.heroesofddd.shared.GameMetaData;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.springframework.stereotype.Component;

@ProcessingGroup("Automation_WhenCreatureRecruitedThenAddToArmy_Processor")
@DisallowReplay
@Component
class WhenCreatureRecruitedThenAddToArmyProcessor {

    private final CommandGateway commandGateway;

    WhenCreatureRecruitedThenAddToArmyProcessor(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @EventHandler
    void react(
            CreatureRecruited event,
            @MetaDataValue(GameMetaData.GAME_ID_KEY) String gameId,
            @MetaDataValue(GameMetaData.PLAYER_ID_KEY) String playerId
    ) {
        try {
            var command = AddCreatureToArmy.command(
                    event.toArmy(),
                    event.creatureId(),
                    event.quantity()
            );
            commandGateway.sendAndWait(command, GameMetaData.with(gameId, playerId));
        } catch (Exception e) {
            var compensatingAction = IncreaseAvailableCreatures.command(
                    event.dwellingId(),
                    event.creatureId(),
                    event.quantity()
            );
            commandGateway.sendAndWait(compensatingAction, GameMetaData.with(gameId, playerId));
        }
    }
}
