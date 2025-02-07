package com.dddheroes.heroesofddd.calendar.write;

import com.dddheroes.heroesofddd.calendar.write.finishday.DayFinished;
import com.dddheroes.heroesofddd.calendar.write.finishday.FinishDay;
import com.dddheroes.heroesofddd.calendar.write.startday.DayStarted;
import com.dddheroes.heroesofddd.calendar.write.startday.StartDay;
import org.axonframework.commandhandling.CommandHandler;
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
        //todo: validate!

        apply(
                DayStarted.event(
                        command.calendarId(),
                        command.month(),
                        command.week(),
                        command.day()
                )
        );
    }

    @CommandHandler
    void handle(FinishDay command) {
        //todo: validate!

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
