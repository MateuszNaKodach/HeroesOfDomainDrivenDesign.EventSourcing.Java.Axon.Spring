package com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures;

import com.dddheroes.heroesofddd.creaturerecruitment.events.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingTest;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import org.axonframework.modelling.entity.AggregateNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

class IncreaseAvailableCreaturesTest extends DwellingTest {

    @Test
    void givenNotBuildDwellingWhenIncreaseAvailableCreaturesThenException() {
        // given
        var givenEvents = List.of();

        // when
        var whenCommand = increaseAvailableCreatures(3);

        // then
        var thenEvent = availableCreaturesChanged(3);
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(AggregateNotFoundException.class);
    }

    @Test
    void givenBuiltDwellingWhenIncreaseAvailableCreaturesThenAvailableCreaturesChanged() {
        // given
        var givenEvents = List.of(
                dwellingBuilt()
        );

        // when
        var whenCommand = increaseAvailableCreatures(3);

        // then
        var thenEvent = availableCreaturesChanged(3);
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .events(thenEvent);
    }

    @Test
    void givenBuiltDwellingWithAvailableCreaturesWhenIncreaseAvailableCreaturesThenAvailableCreaturesChanged() {
        // given
        var givenEvents = List.of(
                dwellingBuilt(),
                availableCreaturesChanged(1)
        );

        // when
        var whenCommand = increaseAvailableCreatures(3);

        // then
        var thenEvent = availableCreaturesChanged(4);
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .events(thenEvent);
    }

    protected IncreaseAvailableCreatures increaseAvailableCreatures(int increaseBy) {
        return IncreaseAvailableCreatures.command(dwellingId.raw(), angelId.raw(), increaseBy);
    }

    private AvailableCreaturesChanged availableCreaturesChanged(int changedTo) {
        return AvailableCreaturesChanged.event(dwellingId, angelId, Amount.of(changedTo));
    }
}
