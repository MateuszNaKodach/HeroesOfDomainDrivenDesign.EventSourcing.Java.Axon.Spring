package com.dddheroes.heroesofddd.creaturerecruitment.events;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.Resources;
import com.dddheroes.heroesofddd.shared.CreatureId;

import java.util.Map;

public record DwellingBuilt(
        String dwellingId,
        String creatureId,
        Map<String, Integer> costPerTroop
) implements DwellingEvent {

    public static DwellingBuilt event(DwellingId dwellingId, CreatureId creatureId, Resources costPerTroop) {
        return new DwellingBuilt(dwellingId.raw(), creatureId.raw(), costPerTroop.raw());
    }
}
