package com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelRepository;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class GetDwellingByIdQueryHandler {
    private final DwellingReadModelRepository dwellingReadModelRepository;

    GetDwellingByIdQueryHandler(DwellingReadModelRepository dwellingReadModelRepository) {
        this.dwellingReadModelRepository = dwellingReadModelRepository;
    }

    @QueryHandler
    Optional<DwellingReadModel> handle(GetDwellingById query){
        return dwellingReadModelRepository.findById(query.dwellingId().raw());
    }
}
