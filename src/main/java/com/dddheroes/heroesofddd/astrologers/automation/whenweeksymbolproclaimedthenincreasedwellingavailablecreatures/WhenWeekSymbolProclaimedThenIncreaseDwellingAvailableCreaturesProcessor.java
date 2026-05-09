package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import com.dddheroes.heroesofddd.astrologers.events.WeekSymbolProclaimed;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.core.annotation.MetadataValue;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.eventhandling.replay.annotation.DisallowReplay;
import org.axonframework.messaging.core.annotation.Namespace;
import org.springframework.stereotype.Component;

@Namespace("ReadModel_Dwelling")
@DisallowReplay
@Component
class WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor {

    private final CommandGateway commandGateway;

    // it may be easier to use live model, but AF4 do not allow me to read events just till some position
    private final BuiltDwellingReadModelRepository repository;

    WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor(
            CommandGateway commandGateway,
            BuiltDwellingReadModelRepository repository
    ) {
        this.commandGateway = commandGateway;
        this.repository = repository;
    }

    @EventHandler
    void react(
            WeekSymbolProclaimed event,
            @MetadataValue(GameMetaData.GAME_ID_KEY) String gameId,
            @MetadataValue(GameMetaData.PLAYER_ID_KEY) String playerId
    ) {
        var creature = event.weekOf();
        var increaseBy = event.growth();
        repository.findAllByGameId(gameId).stream()
                  .filter(dwelling -> dwelling.getCreatureId().equals(creature))
                  .forEach(dwelling -> increaseAvailableCreatures(dwelling, increaseBy, playerId));
    }

    private void increaseAvailableCreatures(BuiltDwellingReadModel dwelling, Integer increaseBy, String playerId) {
        var command = IncreaseAvailableCreatures.command(
                dwelling.getDwellingId(),
                dwelling.getCreatureId(),
                increaseBy
        );
        commandGateway.sendAndWait(command, GameMetaData.with(dwelling.getGameId(), playerId));
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
