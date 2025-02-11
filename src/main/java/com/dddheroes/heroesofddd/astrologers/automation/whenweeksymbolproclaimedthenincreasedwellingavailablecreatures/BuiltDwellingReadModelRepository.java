package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BuiltDwellingReadModelRepository extends JpaRepository<BuiltDwellingReadModel, String> {

    List<BuiltDwellingReadModel> findAllByGameId(String gameId);
}
