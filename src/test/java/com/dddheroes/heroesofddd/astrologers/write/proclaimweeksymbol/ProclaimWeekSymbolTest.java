package com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol;

import com.dddheroes.heroesofddd.astrologers.events.WeekSymbolProclaimed;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersId;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersTest;
import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;
import com.dddheroes.heroesofddd.shared.CreatureId;
import com.dddheroes.heroesofddd.shared.DomainRule;
import org.junit.jupiter.api.*;

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

        // when
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
    void givenWeekSymbolProclaimed_whenProclaimWeekSymbol_ThenException() {
        // given
        var astrologersId = AstrologersId.random();
        var month = 4;
        var week = 2;
        var weekOf = CreatureId.of("black-dragon");
        var growth = +5;
        var givenEvents = List.of(
                WeekSymbolProclaimed.event(
                        astrologersId,
                        MonthWeek.of(month, week),
                        WeekSymbol.of(weekOf, growth)
                )
        );

        // when
        var whenCommand = ProclaimWeekSymbol.command(astrologersId.raw(), month, week, weekOf.raw(), growth);

        // when
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Only one symbol can be proclaimed per week");
    }

    @Test
    void givenWeekSymbolProclaimed_whenProclaimWeekSymbolForPastWeek_ThenException() {
        // given
        var astrologersId = AstrologersId.random();
        var month = 4;
        var week = 2;
        var weekOf = CreatureId.of("cyclops");
        var growth = +5;
        var givenEvents = List.of(
                WeekSymbolProclaimed.event(
                        astrologersId,
                        MonthWeek.of(month, week),
                        WeekSymbol.of(weekOf, growth)
                )
        );

        // when
        var whenCommand = ProclaimWeekSymbol.command(astrologersId.raw(), month, week - 1, weekOf.raw(), growth);

        // when
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Only one symbol can be proclaimed per week");
    }
}
