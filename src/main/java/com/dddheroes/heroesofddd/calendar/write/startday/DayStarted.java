package com.dddheroes.heroesofddd.calendar.write.startday;

import com.dddheroes.heroesofddd.calendar.write.CalendarEvent;
import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.Month;

public record DayStarted(
        String calendarId,
        Integer month,
        Integer week,
        Integer day
) implements CalendarEvent {

    public static DayStarted event(CalendarId calendarId, Month month, Month week, Month day) {
        return new DayStarted(calendarId.raw(), month.raw(), week.raw(), day.raw());
    }
}
