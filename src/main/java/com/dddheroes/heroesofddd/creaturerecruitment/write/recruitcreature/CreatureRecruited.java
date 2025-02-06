package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingEvent;

import java.util.Map;

public record CreatureRecruited(
        String dwellingId,
        String creatureId,
        Integer recruited,
        Map<String, Integer> totalCost
) implements DwellingEvent {

}
