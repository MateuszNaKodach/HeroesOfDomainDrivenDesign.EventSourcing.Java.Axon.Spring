package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingTest;
import com.dddheroes.heroesofddd.creaturerecruitment.events.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.junit.jupiter.api.Test;

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
        // AF5: empty Dwelling materialised by no-arg @EntityCreator → dwellingId is null →
        // explicit OnlyBuiltDwellingCanHaveAvailableCreatures guard at the top of the recruit
        // handler fires. Replaces AF4's AggregateNotFoundException (meaningless to the domain).
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Only built dwelling can have available creatures");
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Recruit creatures not exceed available creatures");
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .events(thenEvent);
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .events(thenEvent);
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .events(thenEvent);
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Recruit creatures not exceed available creatures");
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Recruit creatures not exceed available creatures");
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Recruit creatures not exceed available creatures");
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .events(thenEvent);
    }

    @Test
    void givenDwellingWhenExpectedCostDoesNotMatchActualCostThenException() {
        // given
        var givenEvents = List.of(
                dwellingBuilt(),
                availableCreaturesChanged(1)
        );

        // when
        var whenCommand = recruitCreature(angelId, 1, Resources.from(ResourceType.GOLD, Amount.of(999999)));

        // then
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Recruit cost cannot differ than expected cost");
    }

    private RecruitCreature recruitCreature(int recruit) {
        return recruitCreature(angelId, recruit);
    }

    private RecruitCreature recruitCreature(CreatureId creatureId, int quantity) {
        return recruitCreature(creatureId, quantity, costPerTroop.multiply(Amount.of(quantity)));
    }

    private RecruitCreature recruitCreature(CreatureId creatureId, int quantity, Resources expectedCost) {
        return RecruitCreature.command(dwellingId.raw(), creatureId.raw(), armyId.raw(), quantity, expectedCost.raw());
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