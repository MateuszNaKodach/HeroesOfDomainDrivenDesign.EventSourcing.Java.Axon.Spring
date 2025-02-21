package com.dddheroes.heroesofddd.calendar.write;

import com.dddheroes.heroesofddd.shared.slices.write.Command;

public interface CalendarCommand extends Command {

    CalendarId calendarId();
}
