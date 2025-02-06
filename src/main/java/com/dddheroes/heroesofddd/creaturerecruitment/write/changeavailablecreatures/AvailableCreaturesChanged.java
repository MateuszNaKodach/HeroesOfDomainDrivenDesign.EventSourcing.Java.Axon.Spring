package com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingEvent;

public record AvailableCreaturesChanged(
        String dwellingId,
        String creatureId,
        Integer changedTo
) implements DwellingEvent {

}
