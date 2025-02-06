package com.dddheroes.heroesofddd.creaturerecruitment.read;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateRepository extends JpaRepository<DwellingReadModel.State, String> {

}
