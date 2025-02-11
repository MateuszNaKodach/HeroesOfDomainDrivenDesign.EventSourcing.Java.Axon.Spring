package com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelRepository;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.shared.GameMetaData;
import com.google.common.collect.Streams;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.messaging.annotation.MetaDataValue;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedDeque;

@ProcessingGroup("Read_GetAllDwellings_QueryCache")
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
        var result = Streams.concat(
                                    dwellings.stream(),
                                    cache.stream().filter(it -> it.getGameId().equals(gameId))
                            ) // todo: check ordering
                            .distinct()
                            .toList();
        return new GetAllDwellings.Result(result);
    }

    @EventHandler
    void evolve(DwellingBuilt event, @MetaDataValue(GameMetaData.KEY) String gameId) {
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
