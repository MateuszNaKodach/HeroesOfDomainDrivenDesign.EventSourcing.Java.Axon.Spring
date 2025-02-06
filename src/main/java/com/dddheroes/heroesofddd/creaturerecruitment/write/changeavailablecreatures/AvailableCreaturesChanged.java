package com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingEvent;
import com.dddheroes.heroesofddd.shared.Amount;

public record AvailableCreaturesChanged(
        String dwellingId,
        String creatureId,
        Amount changedTo
) implements DwellingEvent {

}
