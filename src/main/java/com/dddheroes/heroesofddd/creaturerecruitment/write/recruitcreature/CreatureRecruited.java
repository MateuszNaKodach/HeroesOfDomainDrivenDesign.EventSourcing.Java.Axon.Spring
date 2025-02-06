package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingEvent;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.Cost;
import com.dddheroes.heroesofddd.shared.CreatureId;

import java.util.Map;

public record CreatureRecruited(
        String dwellingId,
        String creatureId,
        String toArmy,
        Integer recruited,
        Map<String, Integer> totalCost
) implements DwellingEvent {

    public static CreatureRecruited event(
            DwellingId dwellingId,
            CreatureId creatureId,
            ArmyId toArmy,
            Amount recruited,
            Cost totalCost
    ) {
        return new CreatureRecruited(
                dwellingId.raw(),
                creatureId.raw(),
                toArmy.raw(),
                recruited.raw(),
                totalCost.raw()
        );
    }
}
