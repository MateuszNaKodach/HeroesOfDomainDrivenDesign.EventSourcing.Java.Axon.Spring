package com.dddheroes.heroesofddd.armies.write.removecreature;

import com.dddheroes.heroesofddd.armies.write.ArmyTest;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;
import org.junit.jupiter.api.Test;

import java.util.List;

class RemoveCreatureFromArmyTest extends ArmyTest {

    @Test
    void givenEmptyArmy_WhenRemoveCreatureFromArmy_ThenException() {
        // given
        var givenEvents = List.of();

        // when
        var whenCommand = removeCreatureFromArmy(CreatureIds.angel(), 1);

        // then
        // AF5: with @EntityCreator no-arg, the framework materialises an empty Army and runs
        // the instance handler — the domain rule fires instead of AggregateNotFoundException.
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Can remove only present creatures");
    }

    @Test
    void givenSomeStacksInArmy_WhenRemovePresentCreatureFromArmy_ThenSuccess() {
        // given
        var givenEvents = List.of(
                creatureAddedToArmy(CreatureIds.centaur(), 5),
                creatureAddedToArmy(CreatureIds.bowman(), 99)
        );

        // when
        var whenCommand = removeCreatureFromArmy(CreatureIds.centaur(), 5);

        // then
        var thenEvent = creatureRemovedFromArmy(CreatureIds.centaur(), 5);
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .events(thenEvent);
    }

    @Test
    void givenSomeStacksInArmy_WhenRemoveNotPresentCreatureFromArmy_ThenException() {
        // given
        var givenEvents = List.of(
                creatureAddedToArmy(CreatureIds.centaur(), 5),
                creatureAddedToArmy(CreatureIds.bowman(), 99)
        );

        // when
        var whenCommand = removeCreatureFromArmy(CreatureIds.angel(), 5);

        // then
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Can remove only present creatures");
    }

    @Test
    void givenSomeStacksInArmy_WhenRemoveMoreCreatureThanPresentFromArmy_ThenException() {
        // given
        var givenEvents = List.of(
                creatureAddedToArmy(CreatureIds.centaur(), 5),
                creatureAddedToArmy(CreatureIds.bowman(), 99)
        );

        // when
        var whenCommand = removeCreatureFromArmy(CreatureIds.centaur(), 6);

        // then
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Can remove only present creatures");
    }

    protected RemoveCreatureFromArmy removeCreatureFromArmy(CreatureId creatureId, int quantity) {
        return new RemoveCreatureFromArmy(armyId, creatureId, Amount.of(quantity));
    }
}
