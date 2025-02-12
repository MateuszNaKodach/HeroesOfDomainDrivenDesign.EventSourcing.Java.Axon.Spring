package com.dddheroes.heroesofddd.creaturerecruitment.automation;

import com.dddheroes.heroesofddd.armies.write.addcreature.AddCreatureToArmy;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.CreatureRecruited;
import com.dddheroes.heroesofddd.shared.GameMetaData;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.messaging.unitofwork.AsyncUnitOfWork;
import org.axonframework.messaging.unitofwork.ProcessingContext;
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
    void react(CreatureRecruited event, ProcessingContext processingContext, @MetaDataValue(GameMetaData.KEY) String gameId) {
        var command = AddCreatureToArmy.command(
                event.toArmy(),
                event.creatureId(),
                event.quantity()
        );

        commandGateway.send(command, GameMetaData.withId(gameId), processingContext);
    }
}
