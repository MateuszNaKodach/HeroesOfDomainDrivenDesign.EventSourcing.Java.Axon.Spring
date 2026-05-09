package com.dddheroes.heroesofddd.armies.events;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.eventhandling.annotation.Event;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;

@Event
public record CreatureAddedToArmy(
        @EventTag(key = "Army")
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
