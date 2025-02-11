package com.dddheroes.heroesofddd.calendar.write.finishday;

import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.CalendarTest;
import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;
import com.dddheroes.heroesofddd.calendar.write.startday.DayStarted;
import com.dddheroes.heroesofddd.calendar.write.startday.StartDay;
import com.dddheroes.heroesofddd.shared.DomainRule;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FinishDayTest extends CalendarTest {

    @Test
    void givenNoPreviousDay_WhenFinishDay_ThenException() {
        // given
        var calendarId = CalendarId.random();

        // when
        var whenCommand = FinishDay.command(calendarId.raw(), 1, 1, 1);

        // then
        fixture.givenNoPriorActivity()
               .when(whenCommand)
               .expectException(AggregateNotFoundException.class);
    }

    @Test
    void givenDayStarted_WhenFinishCurrentDay_ThenSuccess() {
        // given
        var calendarId = CalendarId.random();
        var givenEvents = List.of(
                DayStarted.event(calendarId, Month.of(1), Week.of(1), Day.of(1))
        );

        // when
        var whenCommand = FinishDay.command(calendarId.raw(), 1, 1, 1);

        // then
        var thenEvent = DayFinished.event(calendarId, Month.of(1), Week.of(1), Day.of(1));
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }

    @Test
    void givenDayStarted_WhenFinishNotCurrent_ThenException() {
        // given
        var calendarId = CalendarId.random();
        var givenEvents = List.of(
                DayStarted.event(calendarId, Month.of(1), Week.of(1), Day.of(1))
        );

        // when
        var whenCommand = FinishDay.command(calendarId.raw(), 1, 1, 2);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Can only finish current week");
    }
}
