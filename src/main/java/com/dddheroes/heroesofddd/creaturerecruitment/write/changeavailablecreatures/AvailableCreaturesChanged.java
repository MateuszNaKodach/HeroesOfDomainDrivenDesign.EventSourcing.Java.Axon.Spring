package com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingEvent;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.CreatureId;

public record AvailableCreaturesChanged(
        String dwellingId,
        String creatureId,
        Integer changedTo
) implements DwellingEvent {

    public static AvailableCreaturesChanged event(DwellingId dwellingId, CreatureId creatureId, Amount changedTo) {
        return new AvailableCreaturesChanged(dwellingId.raw(), creatureId.raw(), changedTo.raw());
    }
}
