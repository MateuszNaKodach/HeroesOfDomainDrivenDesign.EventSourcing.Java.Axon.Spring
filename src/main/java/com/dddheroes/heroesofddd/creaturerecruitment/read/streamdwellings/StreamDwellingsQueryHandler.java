package com.dddheroes.heroesofddd.creaturerecruitment.read.streamdwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelRepository;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Streaming query example (Axon Framework 4.6+).
 * <p>
 * The handler returns a reactive {@link Flux}. Callers dispatch this with
 * {@code queryGateway.streamingQuery(query, DwellingReadModel.class)}, which returns a
 * {@link org.reactivestreams.Publisher} that emits results lazily once subscribed to. This
 * suits large result sets that should not be materialized into a single list.
 */
@Component
class StreamDwellingsQueryHandler {

    private final DwellingReadModelRepository dwellingReadModelRepository;

    StreamDwellingsQueryHandler(DwellingReadModelRepository dwellingReadModelRepository) {
        this.dwellingReadModelRepository = dwellingReadModelRepository;
    }

    @QueryHandler
    Flux<DwellingReadModel> handle(StreamDwellings query) {
        return Flux.fromIterable(dwellingReadModelRepository.findAllByGameId(query.gameId().raw()));
    }
}
