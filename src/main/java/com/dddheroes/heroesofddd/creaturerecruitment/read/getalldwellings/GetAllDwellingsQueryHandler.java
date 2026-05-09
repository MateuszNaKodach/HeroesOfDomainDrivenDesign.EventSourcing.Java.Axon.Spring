package com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelRepository;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import org.axonframework.messaging.core.annotation.MetadataValue;
import org.axonframework.messaging.core.annotation.Namespace;
import org.axonframework.messaging.core.annotation.SequencingPolicy;
import org.axonframework.messaging.core.sequencing.MetadataSequencingPolicy;
import org.axonframework.messaging.eventhandling.annotation.EventHandler;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

@Namespace("Read_GetAllDwellings_QueryCache")
@SequencingPolicy(type = MetadataSequencingPolicy.class, parameters = GameMetaData.GAME_ID_KEY)
@Component
class GetAllDwellingsQueryHandler {

    private final DwellingReadModelRepository dwellingReadModelRepository;
    private final ConcurrentLinkedDeque<DwellingReadModel> cache = new ConcurrentLinkedDeque<>();

    GetAllDwellingsQueryHandler(DwellingReadModelRepository dwellingReadModelRepository) {
        this.dwellingReadModelRepository = dwellingReadModelRepository;
    }

    @QueryHandler
    GetAllDwellings.Result handle(GetAllDwellings query) {
        var gameId = query.gameId().raw();
        var dwellings = dwellingReadModelRepository.findAllByGameId(gameId);
        var result = Stream.concat(
                        dwellings.stream(),
                        cache.stream().filter(it -> it.getGameId().equals(gameId))
                ) // todo: check ordering
                .distinct()
                .toList();
        return new GetAllDwellings.Result(result);
    }

    @EventHandler
    void evolve(DwellingBuilt event, @MetadataValue(GameMetaData.GAME_ID_KEY) String gameId) {
        while (cache.size() > 20) {
            cache.pollFirst();
        }
        var item = new DwellingReadModel(
                gameId,
                event.dwellingId(),
                event.creatureId(),
                event.costPerTroop(),
                0
        );
        if (!cache.contains(item)) { // todo: check if there are any concurrency issues
            cache.push(item);
        }
    }
}

// without cache
//@Component
//class GetAllDwellingsQueryHandler {
//
//    private final DwellingReadModelRepository dwellingReadModelRepository;
//
//    GetAllDwellingsQueryHandler(DwellingReadModelRepository dwellingReadModelRepository) {
//        this.dwellingReadModelRepository = dwellingReadModelRepository;
//    }
//
//    @QueryHandler
//    GetAllDwellings.Result handle(GetAllDwellings query) {
//        var dwellings = dwellingReadModelRepository.findAll();
//        return new GetAllDwellings.Result(dwellings);
//    }
//}
