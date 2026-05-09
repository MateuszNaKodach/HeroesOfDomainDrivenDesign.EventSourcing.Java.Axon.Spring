package com.dddheroes.heroesofddd.armies.write;

import com.dddheroes.heroesofddd.armies.events.CreatureAddedToArmy;
import com.dddheroes.heroesofddd.armies.events.CreatureRemovedFromArmy;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule;
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.test.fixture.AxonTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class ArmyTest {

    protected final ArmyId armyId = ArmyId.random();

    protected AxonTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = AxonTestFixture.with(EventSourcingConfigurer.create().registerEntity(EventSourcedEntityModule.autodetected(ArmyId.class, Army.class)));
    }

    @AfterEach
    void tearDown() {
        fixture.stop();
    }

    protected CreatureAddedToArmy creatureAddedToArmy(CreatureId creatureId, int quantity) {
        return CreatureAddedToArmy.event(armyId, creatureId, Amount.of(quantity));
    }

    protected CreatureRemovedFromArmy creatureRemovedFromArmy(CreatureId creatureId, int quantity) {
        return CreatureRemovedFromArmy.event(armyId, creatureId, Amount.of(quantity));
    }
}
