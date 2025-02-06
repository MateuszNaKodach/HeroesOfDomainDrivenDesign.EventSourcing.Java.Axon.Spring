package com.dddheroes.heroesofddd.creaturerecruitment.write;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.Cost;
import com.dddheroes.heroesofddd.shared.CreatureId;
import com.dddheroes.heroesofddd.shared.ResourceType;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.List;

class DwellingTest {

    private final DwellingId dwellingId = DwellingId.random();
    private final CreatureId angelId = CreatureId.of("angel");
    private final Cost costPerTroop = Cost
            .resources(ResourceType.GOLD, Amount.of(3000))
            .plus(ResourceType.CRYSTAL, Amount.of(1));

    private AggregateTestFixture<Dwelling> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(Dwelling.class);
    }

    @Test
    void givenNotBuiltDwellingWhenBuildThenBuilt() {
        // given
        var givenEvents = List.of();

        // when
        var whenCommand = buildDwelling();

        // then
        var thenEvent = dwellingBuilt();
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }

    private DwellingBuilt dwellingBuilt() {
        return DwellingBuilt.event(dwellingId, angelId, costPerTroop);
    }

    private BuildDwelling buildDwelling() {
        return BuildDwelling.command(dwellingId.raw(), angelId.raw(), costPerTroop.raw());
    }
}