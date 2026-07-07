package com.dddheroes.heroesofddd.creaturerecruitment.read;

import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.events.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited;
import com.dddheroes.heroesofddd.creaturerecruitment.read.watchdwelling.WatchDwelling;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ResetHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Component;

@ProcessingGroup("ReadModel_Dwelling")
@Component
class DwellingReadModelProjector {

    private final DwellingReadModelRepository repository;
    private final QueryUpdateEmitter queryUpdateEmitter;

    DwellingReadModelProjector(DwellingReadModelRepository repository, QueryUpdateEmitter queryUpdateEmitter) {
        this.repository = repository;
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

    @EventHandler
    void on(DwellingBuilt event, @MetaDataValue(GameMetaData.GAME_ID_KEY) String gameId) {
        var state = new DwellingReadModel(
                gameId,
                event.dwellingId(),
                event.creatureId(),
                event.costPerTroop(),
                0
        );
        repository.save(state);
        emitWatchUpdate(state);
    }

    @EventHandler
    void on(AvailableCreaturesChanged event) {
        repository.findById(event.dwellingId())
                  .map(state -> state.withAvailableCreatures(event.changedTo()))
                  .map(repository::save)
                  .ifPresent(this::emitWatchUpdate);
    }

    @EventHandler
    void on(CreatureRecruited event) {
        repository.findById(event.dwellingId())
                  .map(state -> state.withAvailableCreaturesDecreasedBy(event.quantity()))
                  .map(repository::save)
                  .ifPresent(this::emitWatchUpdate);
    }

    // Emit the just-persisted state to any WatchDwelling subscription query for this dwelling.
    // Emitting from the projecting handler (rather than a separate processor) guarantees the update
    // reflects the value written in this same unit of work — no cross-processor race.
    private void emitWatchUpdate(DwellingReadModel state) {
        queryUpdateEmitter.emit(
                WatchDwelling.class,
                query -> query.dwellingId().raw().equals(state.getDwellingId()),
                state
        );
    }

    @ResetHandler
    void onReset() {
        repository.deleteAll();
    }
}
