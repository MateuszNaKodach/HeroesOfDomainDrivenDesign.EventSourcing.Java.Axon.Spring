package com.dddheroes.heroesofddd.armies.write;

import com.dddheroes.heroesofddd.armies.write.addcreature.AddCreatureToArmy;
import com.dddheroes.heroesofddd.armies.write.addcreature.CreatureAddedToArmy;
import com.dddheroes.heroesofddd.armies.write.removecreature.CreatureRemovedFromArmy;
import com.dddheroes.heroesofddd.armies.write.removecreature.RemoveCreatureFromArmy;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureId;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.HashMap;
import java.util.Map;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
class Army {

    @AggregateIdentifier
    private ArmyId armyId;
    private final Map<CreatureId, Amount> creatureStacks = new HashMap<>();

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    void handle(AddCreatureToArmy command) {
        if (creatureStacks.size() >= 7 && !creatureStacks.containsKey(command.creatureId())) {
            throw new IllegalStateException("Cannot add more than 7 different creature stacks to the army");
        }

        apply(
                CreatureAddedToArmy.event(
                        command.armyId(),
                        command.creatureId(),
                        command.quantity()
                )
        );
    }

    @EventSourcingHandler
    void on(CreatureAddedToArmy event) {
        creatureStacks.merge(new CreatureId(event.creatureId()), new Amount(event.quantity()), Amount::plus);
    }

    @CommandHandler
    void handle(RemoveCreatureFromArmy command) {
        var currentQuantity = creatureStacks.getOrDefault(command.creatureId(), Amount.zero());
        if (currentQuantity.raw() < command.quantity().raw()) {
            throw new IllegalStateException("Not enough creatures to remove");
        }

        apply(
                CreatureRemovedFromArmy.event(
                        command.armyId(),
                        command.creatureId(),
                        command.quantity()
                )
        );
    }

    @EventSourcingHandler
    void on(CreatureRemovedFromArmy event) {
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

    Army() {
        // required by Axon
    }
}
