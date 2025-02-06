package com.dddheroes.heroesofddd.armies.write.addcreature;

import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureId;

public record AddCreatureToArmy(
        ArmyId armyId,
        CreatureId creatureId,
        Amount quantity
) {

    public AddCreatureToArmy {
        if (quantity.raw() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
    }

    public AddCreatureToArmy command(String armyId, String creatureId, int quantity) {
        return new AddCreatureToArmy(new ArmyId(armyId), new CreatureId(creatureId), new Amount(quantity));
    }
}
