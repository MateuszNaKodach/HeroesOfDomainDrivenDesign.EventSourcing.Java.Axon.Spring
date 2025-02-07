package com.dddheroes.heroesofddd.calendar.write.startday;

import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.CalendarTest;
import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;
import org.junit.jupiter.api.*;

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

}
