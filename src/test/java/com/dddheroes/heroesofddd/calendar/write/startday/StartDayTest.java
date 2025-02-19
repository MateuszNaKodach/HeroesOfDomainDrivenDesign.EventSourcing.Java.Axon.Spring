package com.dddheroes.heroesofddd.calendar.write.startday;

import com.dddheroes.heroesofddd.calendar.events.DayStarted;
import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.CalendarTest;
import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;
import com.dddheroes.heroesofddd.calendar.events.DayFinished;
import com.dddheroes.heroesofddd.shared.DomainRule;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    @Test
    void givenLastDayOfFinished_WhenStartDayOfNextWeek_ThenNewWeekStarted() {
        // given
        var calendarId = CalendarId.random();
        var givenEvents = IntStream.rangeClosed(1, 7)
                                   .mapToObj(day -> List.of(
                                           DayStarted.event(calendarId, Month.of(1), Week.of(1), Day.of(day)),
                                           DayFinished.event(calendarId, Month.of(1), Week.of(1), Day.of(day))
                                   ))
                                   .flatMap(List::stream)
                                   .collect(Collectors.toList());

        // when
        var whenCommand = StartDay.command(calendarId.raw(), 1, 2, 1);

        // then
        var thenEvent = DayStarted.event(calendarId, Month.of(1), Week.of(2), Day.of(1));
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }

    @Test
    void givenLastDayOfMonthFinished_WhenStartDayOfNextMonth_ThenNewMonthStarted() {
        // given
        var calendarId = CalendarId.random();
        var givenEvents = IntStream.rangeClosed(1, 4)
                                   .mapToObj(week -> IntStream.rangeClosed(1, 7)
                                                              .mapToObj(day -> List.of(
                                                                      DayStarted.event(calendarId,
                                                                                       Month.of(1),
                                                                                       Week.of(week),
                                                                                       Day.of(day)),
                                                                      DayFinished.event(calendarId,
                                                                                        Month.of(1),
                                                                                        Week.of(week),
                                                                                        Day.of(day))
                                                              ))
                                                              .flatMap(List::stream)
                                                              .collect(Collectors.toList())
                                   )
                                   .flatMap(List::stream)
                                   .collect(Collectors.toList());

        // when
        var whenCommand = StartDay.command(calendarId.raw(), 2, 1, 1);

        // then
        var thenEvent = DayStarted.event(calendarId, Month.of(2), Week.of(1), Day.of(1));
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
    }
}
