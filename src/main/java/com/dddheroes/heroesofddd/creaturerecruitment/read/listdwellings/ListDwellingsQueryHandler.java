package com.dddheroes.heroesofddd.creaturerecruitment.read.listdwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelRepository;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Multi-result query example.
 * <p>
 * The handler returns a reactive {@link Flux} backed directly by the R2DBC repository. Callers
 * dispatch this with {@code queryGateway.queryMany(query, DwellingReadModel.class)}, which collects
 * the emitted results into a {@code CompletableFuture<List<DwellingReadModel>>} (or
 * {@code ReactorQueryGateway.queryMany} for a {@code Mono<List<DwellingReadModel>>}).
 */
@Component
class ListDwellingsQueryHandler {

    private final DwellingReadModelRepository dwellingReadModelRepository;

    ListDwellingsQueryHandler(DwellingReadModelRepository dwellingReadModelRepository) {
        this.dwellingReadModelRepository = dwellingReadModelRepository;
    }

    @QueryHandler
    Flux<DwellingReadModel> handle(ListDwellings query) {
        return dwellingReadModelRepository.findAllByGameId(query.gameId().raw());
    }
}
