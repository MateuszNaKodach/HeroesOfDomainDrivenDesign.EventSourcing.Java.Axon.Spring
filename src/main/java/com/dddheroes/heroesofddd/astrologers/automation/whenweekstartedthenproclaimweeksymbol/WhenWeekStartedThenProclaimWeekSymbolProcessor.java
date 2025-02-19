package com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol;


import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import com.dddheroes.heroesofddd.calendar.events.DayStarted;
import com.dddheroes.heroesofddd.shared.GameMetaData;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.springframework.stereotype.Component;

@ProcessingGroup("Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor")
@DisallowReplay
@Component
class WhenWeekStartedThenProclaimWeekSymbolProcessor {

    public static final int FIRST_DAY_OF_THE_WEEK = 1;

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
    void react(
            DayStarted event,
            @MetaDataValue(GameMetaData.GAME_ID_KEY) String gameId,
            @MetaDataValue(GameMetaData.PLAYER_ID_KEY) String playerId
    ) {
        var isWeekStarted = event.day() == FIRST_DAY_OF_THE_WEEK;
        if (isWeekStarted) {
            var weekSymbol = weekSymbolCalculator.apply(MonthWeek.of(event.month(), event.week()));
            var command = ProclaimWeekSymbol.command(
                    event.calendarId(),
                    event.month(),
                    event.week(),
                    weekSymbol.weekOf().raw(),
                    weekSymbol.growth()
            );
            commandGateway.sendAndWait(command, GameMetaData.with(gameId, playerId));
        }
    }
}
