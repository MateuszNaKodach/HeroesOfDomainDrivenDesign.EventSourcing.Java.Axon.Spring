package com.dddheroes.heroesofddd.armies.write;

import com.dddheroes.heroesofddd.armies.write.addcreature.AddCreatureToArmy;
import com.dddheroes.heroesofddd.armies.write.addcreature.CreatureAddedToArmy;
import com.dddheroes.heroesofddd.armies.write.removecreature.CreatureRemovedFromArmy;
import com.dddheroes.heroesofddd.armies.write.removecreature.RemoveCreatureFromArmy;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureId;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.*;

public class ArmyTest {

    protected final ArmyId armyId = ArmyId.random();

    protected AggregateTestFixture<?> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(Army.class);
    }

    protected CreatureAddedToArmy creatureAddedToArmy(CreatureId creatureId, int quantity) {
        return CreatureAddedToArmy.event(armyId, creatureId, Amount.of(quantity));
    }

    protected AddCreatureToArmy addCreatureToArmy(CreatureId creatureId, int quantity) {
        return new AddCreatureToArmy(armyId, creatureId, Amount.of(quantity));
    }

    protected CreatureRemovedFromArmy creatureRemovedFromArmy(CreatureId creatureId, int quantity) {
        return CreatureRemovedFromArmy.event(armyId, creatureId, Amount.of(quantity));
    }

    protected RemoveCreatureFromArmy removeCreatureFromArmy(CreatureId creatureId, int quantity) {
        return new RemoveCreatureFromArmy(armyId, creatureId, Amount.of(quantity));
    }
}
