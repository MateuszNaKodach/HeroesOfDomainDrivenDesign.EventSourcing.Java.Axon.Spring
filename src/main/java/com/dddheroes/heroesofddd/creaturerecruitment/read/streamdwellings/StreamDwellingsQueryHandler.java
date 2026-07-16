package com.dddheroes.heroesofddd.creaturerecruitment.read.streamdwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelRepository;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Streaming query example.
 * <p>
 * The handler returns a reactive {@link Flux} backed directly by the R2DBC repository, so rows are
 * emitted lazily as the database produces them — nothing is materialized into a list up front.
 * Callers dispatch this with {@code queryGateway.streamingQuery(query, DwellingReadModel.class)}.
 */
@Component
class StreamDwellingsQueryHandler {

    private final DwellingReadModelRepository dwellingReadModelRepository;

    StreamDwellingsQueryHandler(DwellingReadModelRepository dwellingReadModelRepository) {
        this.dwellingReadModelRepository = dwellingReadModelRepository;
    }

    @QueryHandler
    Flux<DwellingReadModel> handle(StreamDwellings query) {
        return dwellingReadModelRepository.findAllByGameId(query.gameId().raw());
    }
}
