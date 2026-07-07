package com.dddheroes.heroesofddd.creaturerecruitment.read;

import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.events.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited;
import com.dddheroes.heroesofddd.creaturerecruitment.read.watchdwelling.WatchDwelling;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import org.axonframework.messaging.core.annotation.MetadataValue;
import org.axonframework.messaging.core.annotation.Namespace;
import org.axonframework.messaging.core.annotation.SequencingPolicy;
import org.axonframework.messaging.core.sequencing.MetadataSequencingPolicy;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.eventhandling.replay.annotation.ResetHandler;
import org.axonframework.messaging.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Component;

@Namespace("ReadModel_Dwelling")
@SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = GameMetaData.GAME_ID_KEY)
@Component
class DwellingReadModelProjector {

    private final DwellingReadModelRepository repository;

    DwellingReadModelProjector(DwellingReadModelRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    void on(DwellingBuilt event, @MetadataValue(GameMetaData.GAME_ID_KEY) String gameId, QueryUpdateEmitter emitter) {
        var state = new DwellingReadModel(
                gameId,
                event.dwellingId(),
                event.creatureId(),
                event.costPerTroop(),
                0
        );
        repository.save(state);
        emitWatchUpdate(emitter, state);
    }

    @EventHandler
    void on(AvailableCreaturesChanged event, QueryUpdateEmitter emitter) {
        repository.findById(event.dwellingId())
                  .map(state -> state.withAvailableCreatures(event.changedTo()))
                  .map(repository::save)
                  .ifPresent(state -> emitWatchUpdate(emitter, state));
    }

    @EventHandler
    void on(CreatureRecruited event, QueryUpdateEmitter emitter) {
        repository.findById(event.dwellingId())
                  .map(state -> state.withAvailableCreaturesDecreasedBy(event.quantity()))
                  .map(repository::save)
                  .ifPresent(state -> emitWatchUpdate(emitter, state));
    }

    // Emit the just-persisted state to any WatchDwelling subscription query for this dwelling.
    // Emitting from the projecting handler (rather than a separate processor) guarantees the update
    // reflects the value written in this same unit of work — no cross-processor race.
    private void emitWatchUpdate(QueryUpdateEmitter emitter, DwellingReadModel state) {
        emitter.emit(
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
