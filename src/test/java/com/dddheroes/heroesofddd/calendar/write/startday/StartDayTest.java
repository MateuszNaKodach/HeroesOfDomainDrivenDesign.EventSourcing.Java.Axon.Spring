package com.dddheroes.heroesofddd.calendar.write.startday;

import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.CalendarTest;
import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;
import com.dddheroes.heroesofddd.calendar.write.finishday.DayFinished;
import com.dddheroes.heroesofddd.shared.DomainRule;
import org.junit.jupiter.api.*;

import java.util.List;

public class StartDayTest extends CalendarTest {

    @Test
    void givenNoPreviousDay_WhenStartDay_ThenSuccess() {
        // given
        var calendarId = CalendarId.random();

        // when
        var whenCommand = StartDay.command(calendarId.raw(), 1, 1, 1);

        // then
        var thenEvent = DayStarted.event(calendarId, Month.of(1), Week.of(1), Day.of(1));
        fixture.givenNoPriorActivity()
                .when(whenCommand)
                .expectEvents(thenEvent);
    }

    @Test
    void givenPreviousDayFinished_WhenStartNextDay_ThenSuccess() {
        // given
        var calendarId = CalendarId.random();
        var givenEvents = List.of(
                DayStarted.event(calendarId, Month.of(1), Week.of(1), Day.of(1)),
                DayFinished.event(calendarId, Month.of(1), Week.of(1), Day.of(1))
        );

        // when
        var whenCommand = StartDay.command(calendarId.raw(), 1, 1, 2);

        // then
        var thenEvent = DayStarted.event(calendarId, Month.of(1), Week.of(1), Day.of(2));
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }

    @Test
    void givenPreviousDayFinished_WhenStartDaySkipping_ThenException() {
        // given
        var calendarId = CalendarId.random();
        var givenEvents = List.of(
                DayStarted.event(calendarId, Month.of(1), Week.of(1), Day.of(1)),
                DayFinished.event(calendarId, Month.of(1), Week.of(1), Day.of(1))
        );

        // when
        var whenCommand = StartDay.command(calendarId.raw(), 1, 1, 3);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Cannot skip days");
    }

}
