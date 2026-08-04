package com.dddheroes.heroesofddd.creaturerecruitment.read.watchdwelling;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelRepository;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.springframework.stereotype.Component;

/**
 * Subscription query example — the initial-result handler.
 * <p>
 * Callers dispatch this with {@code queryGateway.subscriptionQuery(query, DwellingReadModel.class)},
 * which returns a {@link org.reactivestreams.Publisher} emitting this initial result followed by every
 * update emitted for the same dwelling (see the emit calls in
 * {@link com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelProjector}).
 */
@Component
class WatchDwellingQueryHandler {

    private final DwellingReadModelRepository dwellingReadModelRepository;

    WatchDwellingQueryHandler(DwellingReadModelRepository dwellingReadModelRepository) {
        this.dwellingReadModelRepository = dwellingReadModelRepository;
    }

    @QueryHandler
    DwellingReadModel handle(WatchDwelling query) {
        return dwellingReadModelRepository.findById(query.dwellingId().raw()).orElse(null);
    }
}
