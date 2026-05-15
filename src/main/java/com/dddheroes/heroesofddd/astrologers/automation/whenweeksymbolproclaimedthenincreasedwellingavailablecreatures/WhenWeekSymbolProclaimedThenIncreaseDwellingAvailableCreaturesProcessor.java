package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import com.dddheroes.heroesofddd.astrologers.events.WeekSymbolProclaimed;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import org.axonframework.messaging.commandhandling.gateway.CommandDispatcher;
import org.axonframework.messaging.core.annotation.MetadataValue;
import org.axonframework.messaging.core.annotation.Namespace;
import org.axonframework.messaging.core.sequencing.MetadataSequencingPolicy;
import org.axonframework.messaging.core.annotation.SequencingPolicy;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.eventhandling.replay.annotation.DisallowReplay;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Namespace("ReadModel_Dwelling")
@SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = GameMetaData.GAME_ID_KEY)
@DisallowReplay
@Component
class WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor {

    // it may be easier to use live model, but AF4 do not allow me to read events just till some position
    private final BuiltDwellingReadModelRepository repository;

    WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor(
            BuiltDwellingReadModelRepository repository
    ) {
        this.repository = repository;
    }

    @EventHandler
    CompletableFuture<?> react(
            WeekSymbolProclaimed event,
            @MetadataValue(GameMetaData.GAME_ID_KEY) String gameId,
            @MetadataValue(GameMetaData.PLAYER_ID_KEY) String playerId,
            CommandDispatcher commandDispatcher) {
        var creature = event.weekOf();
        var increaseBy = event.growth();
        var futures = repository.findAllByGameId(gameId).stream()
                                .filter(dwelling -> dwelling.getCreatureId().equals(creature))
                                .map(dwelling -> {
                                    var command = IncreaseAvailableCreatures.command(
                                            dwelling.getDwellingId(),
                                            dwelling.getCreatureId(),
                                            increaseBy
                                    );
                                    return commandDispatcher.send(command, GameMetaData.with(dwelling.getGameId(), playerId))
                                                           .getResultMessage();
                                })
                                .toList();
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    @EventHandler
    void on(DwellingBuilt event, @MetadataValue(GameMetaData.GAME_ID_KEY) String gameId) {
        var state = new BuiltDwellingReadModel(
                gameId,
                event.dwellingId(),
                event.creatureId()
        );
        repository.save(state);
    }
}
