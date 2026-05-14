package com.dddheroes.heroesofddd.calendar.events;

import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;
import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.eventhandling.annotation.Event;

@Event
public record DayFinished(
        @EventTag(key = "Calendar")
        String calendarId,
        Integer month,
        Integer week,
        Integer day
) implements CalendarEvent {

    public static DayFinished event(CalendarId calendarId, Month month, Week week, Day day) {
        return new DayFinished(calendarId.raw(), month.raw(), week.raw(), day.raw());
    }
}
