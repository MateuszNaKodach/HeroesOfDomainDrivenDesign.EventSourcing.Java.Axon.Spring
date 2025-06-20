package com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol;

import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import com.dddheroes.heroesofddd.calendar.events.DayStarted;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ReplayStatus;
import org.springframework.stereotype.Component;

@Component
@ProcessingGroup("Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor")
public class WhenWeekStartedThenProclaimWeekSymbolProcessor {

    private final CommandGateway commandGateway;
    private final WeekSymbolCalculator weekSymbolCalculator;

    public WhenWeekStartedThenProclaimWeekSymbolProcessor(CommandGateway commandGateway, WeekSymbolCalculator weekSymbolCalculator) {
        this.commandGateway = commandGateway;
        this.weekSymbolCalculator = weekSymbolCalculator;
    }

    @EventHandler
    public void on(DayStarted event, ReplayStatus replayStatus) {
        if (replayStatus.isReplay()) {
            return; // Do not process replayed events
        }

        // Only trigger when it's the first day of the week
        if (event.day() == 1) {
            var weekSymbol = weekSymbolCalculator.calculateWeekSymbol();
            var growth = weekSymbolCalculator.calculateGrowth();

            var command = ProclaimWeekSymbol.command(
                    event.calendarId(),
                    event.month(),
                    event.week(),
                    weekSymbol.raw(),
                    growth
            );

            commandGateway.sendAndWait(command, GameMetaData.with(event.calendarId()));
        }
    }
} 