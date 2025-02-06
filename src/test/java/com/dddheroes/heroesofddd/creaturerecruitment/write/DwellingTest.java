package com.dddheroes.heroesofddd.creaturerecruitment.write;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.Cost;
import com.dddheroes.heroesofddd.shared.CreatureId;
import com.dddheroes.heroesofddd.shared.DomainRule;
import com.dddheroes.heroesofddd.shared.ResourceType;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.*;

import java.util.List;

public class DwellingTest {

    protected final DwellingId dwellingId = DwellingId.random();
    protected final CreatureId angelId = CreatureId.of("angel");
    protected final Cost costPerTroop = Cost
            .resources(ResourceType.GOLD, Amount.of(3000))
            .plus(ResourceType.CRYSTAL, Amount.of(1));

    protected AggregateTestFixture<Dwelling> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(Dwelling.class);
    }

    protected DwellingBuilt dwellingBuilt() {
        return DwellingBuilt.event(dwellingId, angelId, costPerTroop);
    }

    protected BuildDwelling buildDwelling() {
        return BuildDwelling.command(dwellingId.raw(), angelId.raw(), costPerTroop.raw());
    }
}