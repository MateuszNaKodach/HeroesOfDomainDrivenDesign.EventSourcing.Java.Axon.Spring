package com.dddheroes.heroesofddd.creaturerecruitment.read;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface DwellingReadModelRepository extends R2dbcRepository<DwellingReadModel, String> {

    Flux<DwellingReadModel> findAllByGameId(String gameId);

}
