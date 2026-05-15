package com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol;


import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import com.dddheroes.heroesofddd.calendar.events.DayStarted;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import org.axonframework.messaging.commandhandling.gateway.CommandDispatcher;
import org.axonframework.messaging.core.annotation.MetadataValue;
import org.axonframework.messaging.core.annotation.Namespace;
import org.axonframework.messaging.core.sequencing.MetadataSequencingPolicy;
import org.axonframework.messaging.core.sequencing.annotation.SequencingPolicy;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.eventhandling.replay.annotation.DisallowReplay;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Namespace("Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor")
@SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = GameMetaData.GAME_ID_KEY)
@DisallowReplay
@Component
class WhenWeekStartedThenProclaimWeekSymbolProcessor {

    public static final int FIRST_DAY_OF_THE_WEEK = 1;
    private final WeekSymbolCalculator weekSymbolCalculator;

    WhenWeekStartedThenProclaimWeekSymbolProcessor(
            WeekSymbolCalculator weekSymbolCalculator
    ) {
        this.weekSymbolCalculator = weekSymbolCalculator;
    }

    @EventHandler
    CompletableFuture<?> react(
            DayStarted event, 
            @MetadataValue(GameMetaData.GAME_ID_KEY) String gameId, 
            @MetadataValue(GameMetaData.PLAYER_ID_KEY) String playerId, CommandDispatcher commandDispatcher) {
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
            return commandDispatcher.send(command, GameMetaData.with(gameId, playerId)).getResultMessage();
        }
        return CompletableFuture.completedFuture(null);
    }
}
