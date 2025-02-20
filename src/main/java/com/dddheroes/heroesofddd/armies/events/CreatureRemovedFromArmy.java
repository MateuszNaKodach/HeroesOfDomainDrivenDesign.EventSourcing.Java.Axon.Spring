package com.dddheroes.heroesofddd.armies.events;

import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureId;

public record CreatureRemovedFromArmy(
        String armyId,
        String creatureId,
        Integer quantity
) implements ArmyEvent {

    public static CreatureRemovedFromArmy event(
            ArmyId armyId,
            CreatureId creatureId,
            Amount quantity
    ) {
        return new CreatureRemovedFromArmy(
                armyId.raw(),
                creatureId.raw(),
                quantity.raw()
        );
    }
}
