package com.dddheroes.heroesofddd.armies.write.addcreature;

import com.dddheroes.heroesofddd.armies.write.ArmyTest;
import com.dddheroes.heroesofddd.shared.CreatureIds;
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


}
