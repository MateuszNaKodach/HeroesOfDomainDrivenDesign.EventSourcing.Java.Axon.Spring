package com.dddheroes.heroesofddd.calendar.write.finishday;

import com.dddheroes.heroesofddd.calendar.write.CalendarCommand;
import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;
import org.axonframework.messaging.commandhandling.annotation.Command;
import org.axonframework.modelling.annotation.TargetEntityId;

@Command
public record FinishDay(
        @TargetEntityId
        CalendarId calendarId,
        Month month,
        Week week,
        Day day
) implements CalendarCommand {

    public static FinishDay command(String calendarId, Integer month, Integer week, Integer day) {
        return new FinishDay(CalendarId.of(calendarId), Month.of(month), Week.of(week), Day.of(day));
    }
}
