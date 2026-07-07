package com.dddheroes.heroesofddd.creaturerecruitment.read.listdwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelRepository;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Multi-result query example.
 * <p>
 * The handler returns a {@link List} of results. Callers dispatch this with
 * {@code queryGateway.query(query, ResponseTypes.multipleInstancesOf(DwellingReadModel.class))},
 * which yields a {@code CompletableFuture<List<DwellingReadModel>>}. This is the alternative to
 * wrapping the collection in a dedicated {@code Result} record.
 */
@Component
class ListDwellingsQueryHandler {

    private final DwellingReadModelRepository dwellingReadModelRepository;

    ListDwellingsQueryHandler(DwellingReadModelRepository dwellingReadModelRepository) {
        this.dwellingReadModelRepository = dwellingReadModelRepository;
    }

    @QueryHandler
    List<DwellingReadModel> handle(ListDwellings query) {
        return dwellingReadModelRepository.findAllByGameId(query.gameId().raw());
    }
}
