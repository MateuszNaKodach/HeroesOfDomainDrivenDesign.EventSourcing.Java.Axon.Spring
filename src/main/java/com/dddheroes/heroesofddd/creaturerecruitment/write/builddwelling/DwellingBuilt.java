package com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingCommand;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingEvent;

import java.util.Map;

public record DwellingBuilt(
        String dwellingId,
        String creatureId,
        Map<String, Integer> costPerTroop
) implements DwellingEvent {

}
