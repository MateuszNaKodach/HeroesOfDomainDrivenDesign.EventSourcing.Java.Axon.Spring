package com.dddheroes.heroesofddd.armies.events;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.eventhandling.annotation.Event;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;

@Event
public record CreatureRemovedFromArmy(
        @EventTag(key = "Army")
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
