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
import reactor.core.publisher.Mono;

/**
 * Fully reactive projection: every handler returns a {@link Mono} composed of R2DBC repository
 * operations. The event processor awaits the returned publisher's completion before advancing the
 * token, so the ordering guarantees are the same as with the previous blocking JPA implementation.
 */
@Namespace("ReadModel_Dwelling")
@SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = GameMetaData.GAME_ID_KEY)
@Component
class DwellingReadModelProjector {

    private final DwellingReadModelRepository repository;

    DwellingReadModelProjector(DwellingReadModelRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    Mono<Void> on(DwellingBuilt event, @MetadataValue(GameMetaData.GAME_ID_KEY) String gameId,
                  QueryUpdateEmitter emitter) {
        var state = new DwellingReadModel(
                gameId,
                event.dwellingId(),
                event.creatureId(),
                event.costPerTroop(),
                0
        );
        // findById first keeps redelivery idempotent: an INSERT of an already-projected dwelling
        // would fail on the primary key (JPA's save() used to merge silently).
        return repository.findById(event.dwellingId())
                         .switchIfEmpty(repository.save(state))
                         .doOnNext(saved -> emitWatchUpdate(emitter, saved))
                         .then();
    }

    @EventHandler
    Mono<Void> on(AvailableCreaturesChanged event, QueryUpdateEmitter emitter) {
        return repository.findById(event.dwellingId())
                         .map(state -> state.withAvailableCreatures(event.changedTo()))
                         .flatMap(repository::save)
                         .doOnNext(state -> emitWatchUpdate(emitter, state))
                         .then();
    }

    @EventHandler
    Mono<Void> on(CreatureRecruited event, QueryUpdateEmitter emitter) {
        return repository.findById(event.dwellingId())
                         .map(state -> state.withAvailableCreaturesDecreasedBy(event.quantity()))
                         .flatMap(repository::save)
                         .doOnNext(state -> emitWatchUpdate(emitter, state))
                         .then();
    }

    // Emit the just-persisted state to any WatchDwelling subscription query for this dwelling.
    // Emitting from the projecting handler (rather than a separate processor) guarantees the update
    // reflects the value written in this same unit of work — no cross-processor race. The emitter
    // only registers an after-commit task on the processing context, so it is non-blocking and safe
    // to call from within the reactive pipeline.
    private void emitWatchUpdate(QueryUpdateEmitter emitter, DwellingReadModel state) {
        emitter.emit(
                WatchDwelling.class,
                query -> query.dwellingId().raw().equals(state.getDwellingId()),
                state
        );
    }

    @ResetHandler
    Mono<Void> onReset() {
        return repository.deleteAll();
    }
}
