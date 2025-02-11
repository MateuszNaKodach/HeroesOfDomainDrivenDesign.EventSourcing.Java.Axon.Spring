package com.dddheroes.heroesofddd.creaturerecruitment.read;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DwellingReadModelRepository extends JpaRepository<DwellingReadModel, String> {

    List<DwellingReadModel> findAllByGameId(String gameId);

}
