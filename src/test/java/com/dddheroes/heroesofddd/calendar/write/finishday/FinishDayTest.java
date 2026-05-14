package com.dddheroes.heroesofddd.calendar.write.finishday;

import com.dddheroes.heroesofddd.calendar.events.DayFinished;
import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.CalendarTest;
import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;
import com.dddheroes.heroesofddd.calendar.events.DayStarted;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FinishDayTest extends CalendarTest {

    @Test
    void givenNoPreviousDay_WhenFinishDay_ThenException() {
        // given
        var calendarId = CalendarId.random();

        // when
        var whenCommand = FinishDay.command(calendarId.raw(), 1, 1, 1);

        // then
        fixture.given()
               .noPriorActivity()
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Can only finish current day");
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .events(thenEvent);
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Can only finish current day");
    }
}
