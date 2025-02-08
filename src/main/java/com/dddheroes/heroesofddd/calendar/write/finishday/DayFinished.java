package com.dddheroes.heroesofddd.calendar.write.finishday;

import com.dddheroes.heroesofddd.calendar.write.CalendarEvent;
import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;

public record DayFinished(
        String calendarId,
        Integer month,
        Integer week,
        Integer day
) implements CalendarEvent {

    public static DayFinished event(CalendarId calendarId, Month month, Week week, Day day) {
        return new DayFinished(calendarId.raw(), month.raw(), week.raw(), day.raw());
    }
}
