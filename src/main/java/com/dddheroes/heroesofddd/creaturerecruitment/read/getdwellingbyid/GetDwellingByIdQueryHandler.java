package com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelRepository;
import org.axonframework.messaging.queryhandling.annotation.QueryHandler;
import org.springframework.stereotype.Component;

@Component
class GetDwellingByIdQueryHandler {
    private final DwellingReadModelRepository dwellingReadModelRepository;

    GetDwellingByIdQueryHandler(DwellingReadModelRepository dwellingReadModelRepository) {
        this.dwellingReadModelRepository = dwellingReadModelRepository;
    }

    @QueryHandler
    DwellingReadModel handle(GetDwellingById query){
        return dwellingReadModelRepository.findById(query.dwellingId().raw()).orElse(null);
    }
}
