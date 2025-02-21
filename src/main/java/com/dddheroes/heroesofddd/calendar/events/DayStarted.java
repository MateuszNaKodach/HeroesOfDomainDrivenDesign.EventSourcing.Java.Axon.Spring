package com.dddheroes.heroesofddd.calendar.events;

import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;

public record DayStarted(
        String calendarId,
        Integer month,
        Integer week,
        Integer day
) implements CalendarEvent {

    public static DayStarted event(CalendarId calendarId, Month month, Week week, Day day) {
        return new DayStarted(calendarId.raw(), month.raw(), week.raw(), day.raw());
    }
}
