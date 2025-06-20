package com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol;

import com.dddheroes.heroesofddd.astrologers.events.WeekSymbolProclaimed;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersId;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersTest;
import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ProclaimWeekSymbolTest extends AstrologersTest {

    @Test
    void givenNothing_whenProclaimWeekSymbol_ThenSuccess() {
        // given
        var astrologersId = AstrologersId.random();
        var month = 4;
        var week = 2;
        var weekOf = CreatureId.of("angel");
        var growth = +5;

        // when
        var whenCommand = ProclaimWeekSymbol.command(astrologersId.raw(), month, week, weekOf.raw(), growth);

        // then
        var thenEvent = WeekSymbolProclaimed.event(
                astrologersId,
                MonthWeek.of(month, week),
                WeekSymbol.of(weekOf, growth)
        );
        fixture.givenNoPriorActivity()
               .when(whenCommand)
               .expectEvents(thenEvent);
    }

    @Test
    void givenWeekSymbolAlreadyProclaimed_WhenProclaimWeekSymbolForSameWeek_ThenException() {
        // given
        var astrologersId = AstrologersId.random();
        var month = 4;
        var week = 2;
        var weekOf = CreatureId.of("angel");
        var growth = +5;
        var givenEvents = List.of(
                WeekSymbolProclaimed.event(
                        astrologersId,
                        MonthWeek.of(month, week),
                        WeekSymbol.of(weekOf, growth)
                )
        );

        // when
        var whenCommand = ProclaimWeekSymbol.command(astrologersId.raw(), month, week, "dragon", growth);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Only one symbol per week can be proclaimed");
    }

    @Test
    void givenWeekSymbolForDifferentWeek_WhenProclaimWeekSymbolForAnotherWeek_ThenSuccess() {
        // given
        var astrologersId = AstrologersId.random();
        var weekOf = CreatureId.of("angel");
        var growth = +5;
        var givenEvents = List.of(
                WeekSymbolProclaimed.event(
                        astrologersId,
                        MonthWeek.of(1, 1),
                        WeekSymbol.of(weekOf, growth)
                )
        );

        // when
        var whenCommand = ProclaimWeekSymbol.command(astrologersId.raw(), 1, 2, "dragon", growth);

        // then
        var thenEvent = WeekSymbolProclaimed.event(
                astrologersId,
                MonthWeek.of(1, 2),
                WeekSymbol.of(CreatureId.of("dragon"), growth)
        );
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }
}
