package com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol;


import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import com.dddheroes.heroesofddd.calendar.write.startday.DayStarted;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@ProcessingGroup("Automation_WhenWeekStartedThenProclaimWeekSymbolProcessor_Processor")
@DisallowReplay
@Component
class WhenWeekStartedThenProclaimWeekSymbolProcessor {

    private final CommandGateway commandGateway;
    private final WeekSymbolCalculator weekSymbolCalculator;

    WhenWeekStartedThenProclaimWeekSymbolProcessor(
            CommandGateway commandGateway,
            WeekSymbolCalculator weekSymbolCalculator
    ) {
        this.commandGateway = commandGateway;
        this.weekSymbolCalculator = weekSymbolCalculator;
    }

    @EventHandler
    void react(DayStarted event) {
        var isWeekStarted = event.day() == 1;
        if (isWeekStarted) {
            var weekSymbol = weekSymbolCalculator.apply(MonthWeek.of(event.month(), event.week()));
            var command = ProclaimWeekSymbol.command(
                    event.calendarId(),
                    event.month(),
                    event.week(),
                    weekSymbol.weekOf().raw(),
                    weekSymbol.growth()
            );
            commandGateway.sendAndWait(command);
        }
    }
}
