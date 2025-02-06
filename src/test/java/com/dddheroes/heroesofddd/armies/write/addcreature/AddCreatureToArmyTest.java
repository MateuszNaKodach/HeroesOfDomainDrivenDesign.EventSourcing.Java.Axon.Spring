package com.dddheroes.heroesofddd.armies.write.addcreature;

import com.dddheroes.heroesofddd.armies.write.ArmyTest;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.shared.DomainRule;
import org.junit.jupiter.api.*;

import java.util.List;

public class AddCreatureToArmyTest extends ArmyTest {

    @Test
    void givenEmptyArmy_WhenAddCreatureToArmy_ThenSuccess() {
        // given
        var givenEvents = List.of();

        // when
        var whenCommand = addCreatureToArmy(CreatureIds.angel(), 1);

        // then
        var thenEvent = creatureAddedToArmy(CreatureIds.angel(), 1);
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }

    @Test
    void givenSomeStacksInArmy_WhenAddCreatureToArmy_ThenSuccess() {
        // given
        var givenEvents = List.of(
                creatureAddedToArmy(CreatureIds.centaur(), 5),
                creatureAddedToArmy(CreatureIds.bowman(), 99)
        );

        // when
        var whenCommand = addCreatureToArmy(CreatureIds.angel(), 1);

        // then
        var thenEvent = creatureAddedToArmy(CreatureIds.angel(), 1);
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }

    @Test
    void givenArmyWithMaxCreatureStacks_WhenAddAnotherCreature_ThenException() {
        // given
        var givenEvents = List.of(
                creatureAddedToArmy(CreatureIds.centaur(), 5),
                creatureAddedToArmy(CreatureIds.angel(), 1),
                creatureAddedToArmy(CreatureIds.archAngel(), 3),
                creatureAddedToArmy(CreatureIds.blackDragon(), 9),
                creatureAddedToArmy(CreatureIds.redDragon(), 15),
                creatureAddedToArmy(CreatureIds.bowman(), 12),
                creatureAddedToArmy(CreatureIds.behemoth(), 11)
        );

        // when
        var whenCommand = addCreatureToArmy(CreatureIds.phoenix(), 3);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Can have max 7 different creature stacks in the army");
    }

    @Test
    void givenArmyWithMaxCreatureStacks_WhenAddPresentCreature_ThenSuccess() {
        // given
        var givenEvents = List.of(
                creatureAddedToArmy(CreatureIds.centaur(), 5),
                creatureAddedToArmy(CreatureIds.angel(), 1),
                creatureAddedToArmy(CreatureIds.archAngel(), 3),
                creatureAddedToArmy(CreatureIds.blackDragon(), 9),
                creatureAddedToArmy(CreatureIds.redDragon(), 15),
                creatureAddedToArmy(CreatureIds.bowman(), 12),
                creatureAddedToArmy(CreatureIds.behemoth(), 11)
        );

        // when
        var whenCommand = addCreatureToArmy(CreatureIds.archAngel(), 5);

        // then
        var thenEvent = creatureAddedToArmy(CreatureIds.archAngel(), 5);
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }


}
