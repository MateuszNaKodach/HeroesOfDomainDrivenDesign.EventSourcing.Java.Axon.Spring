package com.dddheroes.heroesofddd.armies.write.removecreature;

import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record RemoveCreatureFromArmy(
        @TargetAggregateIdentifier
        ArmyId armyId,
        CreatureId creatureId,
        Amount quantity
) {

    public RemoveCreatureFromArmy {
        if (quantity.raw() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
    }

    public RemoveCreatureFromArmy command(String armyId, String creatureId, int quantity) {
        return new RemoveCreatureFromArmy(new ArmyId(armyId), new CreatureId(creatureId), new Amount(quantity));
    }
}
