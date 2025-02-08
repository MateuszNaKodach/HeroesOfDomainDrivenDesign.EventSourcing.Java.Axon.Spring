package com.dddheroes.heroesofddd.calendar.write;

import com.dddheroes.heroesofddd.calendar.write.finishday.CanOnlyFinishCurrentDay;
import com.dddheroes.heroesofddd.calendar.write.finishday.DayFinished;
import com.dddheroes.heroesofddd.calendar.write.finishday.FinishDay;
import com.dddheroes.heroesofddd.calendar.write.startday.CannotSkipDays;
import com.dddheroes.heroesofddd.calendar.write.startday.DayStarted;
import com.dddheroes.heroesofddd.calendar.write.startday.StartDay;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
class Calendar {

    @AggregateIdentifier
    private CalendarId calendarId;
    private Month currentMonth;
    private Week currentWeek;
    private Day currentDay;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
        // performance downside in comparison to constructor
    void handle(StartDay command) {
        new CannotSkipDays(command, currentMonth, currentWeek, currentDay).verify();

        apply(
                DayStarted.event(
                        command.calendarId(),
                        command.month(),
                        command.week(),
                        command.day()
                )
        );
    }

    @EventSourcingHandler
    void on(DayStarted event) {
        calendarId = new CalendarId(event.calendarId());
        currentMonth = new Month(event.month());
        currentWeek = new Week(event.week());
        currentDay = new Day(event.day());
    }

    @CommandHandler
    void handle(FinishDay command) {
        new CanOnlyFinishCurrentDay(command, currentMonth, currentWeek, currentDay).verify();

        apply(
                DayFinished.event(
                        command.calendarId(),
                        command.month(),
                        command.week(),
                        command.day()
                )
        );
    }

    Calendar() {
        // required by Axon
    }
}
