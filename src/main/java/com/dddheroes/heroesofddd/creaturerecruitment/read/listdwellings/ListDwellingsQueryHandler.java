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
 * which yields a {@code CompletableFuture<List<DwellingReadModel>>}.
 * <p>
 * Over a serializing query bus (e.g. Axon Server) this relies on the message serializer carrying
 * generic-collection element types — see the default-typing {@code messageSerializer} in
 * {@link com.dddheroes.heroesofddd.shared.infrastructure.SerializationConfiguration}. Without it,
 * Jackson erases the element type and the response deserializes into an untyped {@code ArrayList},
 * failing {@code MultipleInstancesResponseType} conversion.
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
