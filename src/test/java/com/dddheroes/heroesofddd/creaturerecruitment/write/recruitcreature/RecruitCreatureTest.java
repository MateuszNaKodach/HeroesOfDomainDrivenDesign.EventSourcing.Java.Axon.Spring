package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingTest;
import com.dddheroes.heroesofddd.shared.ArmyId;
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
        var toArmy = ArmyId.random();
        var whenCommand = recruitCreature(1);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(AggregateNotFoundException.class);
        // todo: I don't like it exception is not from domain
        //       .expectException(DomainRule.ViolatedException.class)
        //       .expectExceptionMessage("Only not built building can be build");
    }

    private RecruitCreature recruitCreature(int recruit) {
        return RecruitCreature.command(dwellingId.raw(), angelId.raw(), armyId.raw(), recruit);
    }
}