package com.dddheroes.heroesofddd.creaturerecruitment.automation;

import com.dddheroes.heroesofddd.armies.write.addcreature.AddCreatureToArmy;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.CreatureRecruited;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
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
    void on(CreatureRecruited event) {
        var command = AddCreatureToArmy.command(
                event.toArmy(),
                event.creatureId(),
                event.quantity()
        );

        commandGateway.sendAndWait(command);
    }
}
