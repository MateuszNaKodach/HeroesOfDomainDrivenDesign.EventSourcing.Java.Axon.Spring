package com.dddheroes.heroesofddd.armies.write;

import com.dddheroes.heroesofddd.armies.events.CreatureAddedToArmy;
import com.dddheroes.heroesofddd.armies.events.CreatureRemovedFromArmy;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
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

    protected CreatureRemovedFromArmy creatureRemovedFromArmy(CreatureId creatureId, int quantity) {
        return CreatureRemovedFromArmy.event(armyId, creatureId, Amount.of(quantity));
    }
}
