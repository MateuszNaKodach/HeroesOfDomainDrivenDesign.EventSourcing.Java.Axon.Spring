package com.dddheroes.heroesofddd.calendar.write;

import com.dddheroes.heroesofddd.calendar.write.finishday.CanOnlyFinishCurrentDay;
import com.dddheroes.heroesofddd.calendar.events.DayFinished;
import com.dddheroes.heroesofddd.calendar.write.finishday.FinishDay;
import com.dddheroes.heroesofddd.calendar.write.startday.CannotSkipDays;
import com.dddheroes.heroesofddd.calendar.events.DayStarted;
import com.dddheroes.heroesofddd.calendar.write.startday.StartDay;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;

@EventSourced(tagKey = "Calendar", idType = CalendarId.class)
class Calendar {

    private CalendarId calendarId;
    private Month currentMonth;
    private Week currentWeek;
    private Day currentDay;

    @CommandHandler
        // performance downside in comparison to constructor
    void decide(StartDay command, EventAppender eventAppender) {
        new CannotSkipDays(command, currentMonth, currentWeek, currentDay).verify();

        eventAppender.append(DayStarted.event(
                command.calendarId(),
                command.month(),
                command.week(),
                command.day()
        ));
    }

    @EventSourcingHandler
    void evolve(DayStarted event) {
        calendarId = new CalendarId(event.calendarId());
        currentMonth = new Month(event.month());
        currentWeek = new Week(event.week());
        currentDay = new Day(event.day());
    }

    @CommandHandler
    void decide(FinishDay command, EventAppender eventAppender) {
        new CanOnlyFinishCurrentDay(command, currentMonth, currentWeek, currentDay).verify();

        eventAppender.append(DayFinished.event(
                command.calendarId(),
                command.month(),
                command.week(),
                command.day()
        ));
    }

    @EntityCreator
    Calendar() {
        // required by Axon
    }
}
