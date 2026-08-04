package com.dddheroes.heroesofddd.creaturerecruitment.read.watchdwelling;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelRepository;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

/**
 * Subscription query example — the initial-result handler.
 * <p>
 * Callers dispatch this with {@code queryGateway.subscriptionQuery(...)}, which returns a
 * {@code SubscriptionQueryResult} whose initial result comes from this handler and whose updates
 * are emitted for the same dwelling (see the emit calls in
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
