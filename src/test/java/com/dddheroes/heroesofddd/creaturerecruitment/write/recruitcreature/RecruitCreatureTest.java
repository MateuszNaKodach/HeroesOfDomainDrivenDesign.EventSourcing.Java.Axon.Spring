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
//               .expectException(AggregateNotFoundException.class);
        // todo: I don't like it exception is not from domain, AggregateNotFoundException is meaningless
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Only not built building can be build");
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

    @Test
    void givenDwellingWithRecruitedAllAvailableCreaturesWhenRecruitCreatureThenException() {
        // given
        var givenEvents = List.of(
                dwellingBuilt(),
                availableCreaturesChanged(3),
                creatureRecruited(2),
                availableCreaturesChanged(4),
                creatureRecruited(4)
        );

        // when
        var whenCommand = recruitCreature(3);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Recruit creatures not exceed available creatures");
    }

    @Test
    void givenDwellingWithRecruitedSomeAvailableCreaturesAnd1LeftWhenRecruit1CreatureThenRecruited() {
        // given
        var givenEvents = List.of(
                dwellingBuilt(),
                availableCreaturesChanged(4),
                creatureRecruited(3)
        );

        // when
        var whenCommand = recruitCreature(1);

        // then
        var thenEvent = creatureRecruited(1);
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }

    private RecruitCreature recruitCreature(int recruit) {
        return recruitCreature(angelId, recruit);
    }

    private RecruitCreature recruitCreature(CreatureId creatureId, int quantity) {
        return RecruitCreature.command(dwellingId.raw(), creatureId.raw(), armyId.raw(), quantity);
    }

    private CreatureRecruited creatureRecruited(int quantity) {
        return CreatureRecruited.event(dwellingId,
                                       angelId,
                                       armyId,
                                       Amount.of(quantity),
                                       costPerTroop.multiply(Amount.of(quantity)));
    }

    private AvailableCreaturesChanged availableCreaturesChanged(int changedTo) {
        return AvailableCreaturesChanged.event(dwellingId, angelId, Amount.of(changedTo));
    }
}