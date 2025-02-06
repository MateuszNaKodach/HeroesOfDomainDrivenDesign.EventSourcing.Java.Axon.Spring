package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingTest;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureId;
import com.dddheroes.heroesofddd.shared.DomainRule;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.junit.jupiter.api.*;

import java.util.List;

class RecruitCreatureTest extends DwellingTest {

    private final ArmyId armyId = ArmyId.random();

    @Test
    void givenNotBuiltDwellingWhenRecruitCreatureThenException() {
        // given
        var givenEvents = List.of();

        // when
        var whenCommand = recruitCreature(1);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(AggregateNotFoundException.class);
        // todo: I don't like it exception is not from domain
        //       .expectException(DomainRule.ViolatedException.class)
        //       .expectExceptionMessage("Only not built building can be build");
    }

    @Test
    void givenBuiltButEmptyDwellingWhenRecruitCreatureThenException() {
        // given
        var givenEvents = List.of(
                dwellingBuilt()
        );

        // when
        var whenCommand = recruitCreature(1);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Recruit creatures not exceed available creatures");
    }

    @Test
    void givenDwellingWith1CreatureWhenRecruit1CreatureThenRecruited() {
        // given
        var givenEvents = List.of(
                dwellingBuilt(),
                availableCreaturesChanged(1)
        );

        // when
        var whenCommand = recruitCreature(1);

        // then
        var thenEvent = creatureRecruited(1);
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }

    @Test
    void givenDwellingWith2CreaturesWhenRecruit2CreaturesThenRecruited() {
        // given
        var givenEvents = List.of(
                dwellingBuilt(),
                availableCreaturesChanged(2)
        );

        // when
        var whenCommand = recruitCreature(2);

        // then
        var thenEvent = creatureRecruited(2);
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }

    @Test
    void givenDwellingWith4CreaturesWhenRecruit3CreaturesThenRecruited() {
        // given
        var givenEvents = List.of(
                dwellingBuilt(),
                availableCreaturesChanged(3),
                availableCreaturesChanged(4)
        );

        // when
        var whenCommand = recruitCreature(3);

        // then
        var thenEvent = creatureRecruited(3);
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }

    @Test
    void givenDwellingWith5CreaturesWhenRecruit6CreaturesThenException() {
        // given
        var givenEvents = List.of(
                dwellingBuilt(),
                availableCreaturesChanged(5)
        );

        // when
        var whenCommand = recruitCreature(6);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Recruit creatures not exceed available creatures");
    }


    @Test
    void givenDwellingWhenRecruitCreatureNotFromThisDwellingThenException() {
        // given
        var givenEvents = List.of(
                dwellingBuilt(),
                availableCreaturesChanged(1)
        );

        // when
        var anotherCreatureId = CreatureId.of("black-dragon");
        var whenCommand = recruitCreature(anotherCreatureId, 1);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Recruit creatures not exceed available creatures");
    }

    private RecruitCreature recruitCreature(int recruit) {
        return recruitCreature(angelId, recruit);
    }

    private RecruitCreature recruitCreature(CreatureId creatureId, int recruit) {
        return RecruitCreature.command(dwellingId.raw(), creatureId.raw(), armyId.raw(), recruit);
    }

    private CreatureRecruited creatureRecruited(int recruit) {
        return CreatureRecruited.event(dwellingId,
                                       angelId,
                                       armyId,
                                       Amount.of(recruit),
                                       costPerTroop.multiply(Amount.of(recruit)));
    }

    private AvailableCreaturesChanged availableCreaturesChanged(int changedTo) {
        return AvailableCreaturesChanged.event(dwellingId, angelId, Amount.of(changedTo));
    }

    /**
     *
     @Test
     fun `given Dwelling with recruited all available creatures at once, when recruit creature, then nothing`() {
     // given
     val givenEvents = listOf(
     DwellingBuilt(dwellingId, angelId, angelCostPerTroop),
     AvailableCreaturesChanged(dwellingId, angelId, Amount.of(3)),
     CreatureRecruited(
     dwellingId, angelId, Amount.of(3), resources(GOLD to 9000, CRYSTAL to 3), armyId
     ),
     )

     // when
     val whenCommand = RecruitCreature(dwellingId, archangelId, Amount.of(1), armyId)

     // then
     val thenEvents = decide(givenEvents, whenCommand)
     assertThat(thenEvents).isEmpty()
     }

     @Test
     fun `given Dwelling with recruited all available creatures, when recruit creature, then nothing`() {
     // given
     val givenEvents = listOf(
     DwellingBuilt(dwellingId, angelId, angelCostPerTroop),
     AvailableCreaturesChanged(dwellingId, angelId, Amount.of(3)),
     CreatureRecruited(
     dwellingId, angelId, Amount.of(2), resources(GOLD to 6000, CRYSTAL to 2), armyId
     ),
     CreatureRecruited(
     dwellingId, angelId, Amount.of(1), resources(GOLD to 3000, CRYSTAL to 1), armyId
     ),
     )

     // when
     val whenCommand = RecruitCreature(dwellingId, archangelId, Amount.of(1), armyId)

     // then
     val thenEvents = decide(givenEvents, whenCommand)
     assertThat(thenEvents).isEmpty()
     }

     @Test
     fun `given Dwelling with recruited some available creatures and 1 left, when recruit 1 creature, then recruited`() {
     // given
     val givenEvents = listOf(
     DwellingBuilt(dwellingId, angelId, angelCostPerTroop),
     AvailableCreaturesChanged(dwellingId, angelId, changedTo = Amount.of(4)),
     CreatureRecruited(
     dwellingId, angelId, recruited = Amount.of(3), totalCost = resources(GOLD to 9000, CRYSTAL to 3), armyId
     ),
     )

     // when
     val whenCommand = RecruitCreature(dwellingId, angelId, Amount.of(1), armyId)

     // then
     val thenEvents = decide(givenEvents, whenCommand)
     val expectedEvent = CreatureRecruited(
     dwellingId, angelId, recruited = Amount.of(1), totalCost = resources(GOLD to 3000, CRYSTAL to 1), armyId
     )
     assertThat(thenEvents).containsExactly(expectedEvent)
     }

     @Test
     fun `given build Dwelling, when increase available creatures, then available creatures changed`() {
     // given
     val givenEvents = listOf(
     DwellingBuilt(dwellingId, angelId, angelCostPerTroop)
     )

     // when
     val whenCommand = IncreaseAvailableCreatures(dwellingId, angelId, Amount.of(3))

     // then
     val thenEvents = decide(givenEvents, whenCommand)
     val expectedEvent = AvailableCreaturesChanged(dwellingId, angelId, changedTo = Amount.of(3))
     assertThat(thenEvents).containsExactly(expectedEvent)
     }

     */
}