package com.dddheroes.heroesofddd.calendar.write.startday;

import com.dddheroes.heroesofddd.calendar.write.CalendarCommand;
import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record StartDay(
        @TargetAggregateIdentifier
        CalendarId calendarId,
        Month month,
        Week week,
        Day day
) implements CalendarCommand {

    public static StartDay command(String calendarId, Integer month, Integer week, Integer day) {
        return new StartDay(CalendarId.of(calendarId), Month.of(month), Week.of(week), Day.of(day));
    }
}
