package com.dddheroes.heroesofddd.creaturerecruitment.write;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.*;

public class DwellingTest {

    protected final DwellingId dwellingId = DwellingId.random();
    protected final CreatureId angelId = CreatureId.of("angel");
    protected final Resources costPerTroop = Resources
            .from(ResourceType.GOLD, Amount.of(3000))
            .plus(ResourceType.GEMS, Amount.of(1));

    protected AggregateTestFixture<?> fixture;

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