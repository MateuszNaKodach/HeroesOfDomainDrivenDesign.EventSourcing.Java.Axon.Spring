package com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling;

import java.util.Map;

public record DwellingBuilt(
        String dwellingId,
        String creatureId,
        Map<String, Integer> costPerTroop
) {

}
