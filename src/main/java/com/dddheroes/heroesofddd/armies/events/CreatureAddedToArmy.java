package com.dddheroes.heroesofddd.armies.events;

import com.dddheroes.heroesofddd.armies.write.ArmyEvent;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;

public record CreatureAddedToArmy(
        String armyId,
        String creatureId,
        Integer quantity
) implements ArmyEvent {

    public static CreatureAddedToArmy event(
            ArmyId armyId,
            CreatureId creatureId,
            Amount quantity
    ) {
        return new CreatureAddedToArmy(
                armyId.raw(),
                creatureId.raw(),
                quantity.raw()
        );
    }
}
