package com.dddheroes.heroesofddd.creaturerecruitment.read.dwelling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface DwellingReadModelStateRepository extends JpaRepository<DwellingReadModel, String> {

}
