package com.dddheroes.heroesofddd.armies.write;

import com.dddheroes.heroesofddd.armies.write.addcreature.AddCreatureToArmy;
import com.dddheroes.heroesofddd.armies.events.CreatureAddedToArmy;
import com.dddheroes.heroesofddd.armies.write.addcreature.CanHaveMax7CreatureStacksInArmy;
import com.dddheroes.heroesofddd.armies.events.CreatureRemovedFromArmy;
import com.dddheroes.heroesofddd.armies.write.removecreature.CanRemoveOnlyPresentCreatures;
import com.dddheroes.heroesofddd.armies.write.removecreature.RemoveCreatureFromArmy;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;

import java.util.HashMap;
import java.util.Map;

// todo: probably we should model events ArmyEstablished and ArmyDestroyed, more on that on Event Model
@EventSourced(tagKey = "Army", idType = ArmyId.class)
class Army {

    private ArmyId armyId;
    private final Map<CreatureId, Amount> creatureStacks = new HashMap<>();

    @CommandHandler // performance downside in comparison to constructor
    void decide(AddCreatureToArmy command, EventAppender eventAppender) {
        new CanHaveMax7CreatureStacksInArmy(command.creatureId(), creatureStacks).verify();

        eventAppender.append(CreatureAddedToArmy.event(
                command.armyId(),
                command.creatureId(),
                command.quantity()
        ));
    }

    @EventSourcingHandler
    void evolve(CreatureAddedToArmy event) {
        this.armyId = new ArmyId(event.armyId());
        creatureStacks.merge(new CreatureId(event.creatureId()), new Amount(event.quantity()), Amount::plus);
    }

    @CommandHandler
    void decide(RemoveCreatureFromArmy command, EventAppender eventAppender) {
        new CanRemoveOnlyPresentCreatures(command.creatureId(), command.quantity(), creatureStacks).verify();

        eventAppender.append(CreatureRemovedFromArmy.event(
                command.armyId(),
                command.creatureId(),
                command.quantity()
        ));
    }

    @EventSourcingHandler
    void evolve(CreatureRemovedFromArmy event) {
        var creatureId = new CreatureId(event.creatureId());
        var currentQuantity = creatureStacks.get(creatureId);
        var removedQuantity = new Amount(event.quantity());
        if (currentQuantity.equals(removedQuantity)) {
            creatureStacks.remove(creatureId);
        } else {
            creatureStacks.merge(
                    creatureId,
                    removedQuantity,
                    Amount::minus
            );
        }
    }

    @EntityCreator
    Army() {
        // required by Axon
    }
}
