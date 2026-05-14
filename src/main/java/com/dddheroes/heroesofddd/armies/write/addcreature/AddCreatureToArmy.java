package com.dddheroes.heroesofddd.armies.write.addcreature;

import com.dddheroes.heroesofddd.armies.write.ArmyCommand;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import org.axonframework.messaging.commandhandling.annotation.Command;
import org.axonframework.modelling.annotation.TargetEntityId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;

@Command
public record AddCreatureToArmy(
        @TargetEntityId
        ArmyId armyId,
        CreatureId creatureId,
        Amount quantity
) implements ArmyCommand {

    public AddCreatureToArmy {
        if (quantity.raw() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
    }

    public static AddCreatureToArmy command(String armyId, String creatureId, int quantity) {
        return new AddCreatureToArmy(new ArmyId(armyId), new CreatureId(creatureId), new Amount(quantity));
    }
}
