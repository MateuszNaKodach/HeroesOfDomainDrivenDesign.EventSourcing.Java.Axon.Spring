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
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@EventSourced(tagKey = "Calendar", idType = CalendarId.class)
class Calendar {

    private CalendarId calendarId;
    private Month currentMonth;
    private Week currentWeek;
    private Day currentDay;

    static void decide(
            StartDay command,
            @Nullable Calendar calendar,
            EventAppender eventAppender
    ) {
        new CannotSkipDays(
                command,
                calendar == null ? null : calendar.currentMonth,
                calendar == null ? null : calendar.currentWeek,
                calendar == null ? null : calendar.currentDay
        ).verify();

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

    static void decide(
            FinishDay command,
            @Nullable Calendar calendar,
            EventAppender eventAppender
    ) {
        new CanOnlyFinishCurrentDay(
                command,
                calendar == null ? null : calendar.currentMonth,
                calendar == null ? null : calendar.currentWeek,
                calendar == null ? null : calendar.currentDay
        ).verify();

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

@Component
class CalendarCommandHandler {

    @CommandHandler
    void decide(StartDay command, @InjectEntity @Nullable Calendar calendar, EventAppender eventAppender) {
        Calendar.decide(command, calendar, eventAppender);
    }

    @CommandHandler
    void decide(FinishDay command, @InjectEntity @Nullable Calendar calendar, EventAppender eventAppender) {
        Calendar.decide(command, calendar, eventAppender);
    }
}
