package com.dddheroes.heroesofddd.armies.write;

import com.dddheroes.heroesofddd.armies.write.addcreature.AddCreatureToArmy;
import com.dddheroes.heroesofddd.armies.events.CreatureAddedToArmy;
import com.dddheroes.heroesofddd.armies.write.addcreature.CanHaveMax7CreatureStacksInArmy;
import com.dddheroes.heroesofddd.armies.events.CreatureRemovedFromArmy;
import com.dddheroes.heroesofddd.armies.write.removecreature.CanRemoveOnlyPresentCreatures;
import com.dddheroes.heroesofddd.armies.write.removecreature.RemoveCreatureFromArmy;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.HashMap;
import java.util.Map;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

// todo: probably we should model events ArmyEstablished and ArmyDestroyed, more on that on Event Model
@Aggregate
class Army {

    @AggregateIdentifier
    private ArmyId armyId;
    private final Map<CreatureId, Amount> creatureStacks = new HashMap<>();

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING) // performance downside in comparison to constructor
    void decide(AddCreatureToArmy command) {
        new CanHaveMax7CreatureStacksInArmy(command.creatureId(), creatureStacks).verify();

        apply(
                CreatureAddedToArmy.event(
                        command.armyId(),
                        command.creatureId(),
                        command.quantity()
                )
        );
    }

    @EventSourcingHandler
    void evolve(CreatureAddedToArmy event) {
        this.armyId = new ArmyId(event.armyId());
        creatureStacks.merge(new CreatureId(event.creatureId()), new Amount(event.quantity()), Amount::plus);
    }

    @CommandHandler
    void decide(RemoveCreatureFromArmy command) {
        new CanRemoveOnlyPresentCreatures(command.creatureId(), command.quantity(), creatureStacks).verify();

        apply(
                CreatureRemovedFromArmy.event(
                        command.armyId(),
                        command.creatureId(),
                        command.quantity()
                )
        );
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

    Army() {
        // required by Axon
    }
}
