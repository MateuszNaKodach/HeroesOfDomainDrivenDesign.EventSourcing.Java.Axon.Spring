package com.dddheroes.heroesofddd.armies.write.removecreature;

import com.dddheroes.heroesofddd.armies.write.ArmyTest;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.CreatureId;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.shared.DomainRule;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.junit.jupiter.api.*;

import java.util.List;

class RemoveCreatureFromArmyTest extends ArmyTest {

    @Test
    void givenEmptyArmy_WhenRemoveCreatureFromArmy_ThenException() {
        // given
        var givenEvents = List.of();

        // when
        var whenCommand = removeCreatureFromArmy(CreatureIds.angel(), 1);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(AggregateNotFoundException.class);
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
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
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
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Can remove only present creatures");
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
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Can remove only present creatures");
    }

    protected RemoveCreatureFromArmy removeCreatureFromArmy(CreatureId creatureId, int quantity) {
        return new RemoveCreatureFromArmy(armyId, creatureId, Amount.of(quantity));
    }
}
