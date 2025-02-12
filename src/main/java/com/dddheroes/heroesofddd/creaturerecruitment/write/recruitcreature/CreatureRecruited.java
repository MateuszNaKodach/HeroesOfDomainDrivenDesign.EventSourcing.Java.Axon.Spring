package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingEvent;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.dcb.EventTag;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.Resources;
import com.dddheroes.heroesofddd.shared.CreatureId;

import java.util.Map;

public record CreatureRecruited(
        @EventTag
        String dwellingId,
        String creatureId,
        @EventTag(name = "armyId")
        String toArmy,
        Integer quantity,
        Map<String, Integer> totalCost
) implements DwellingEvent {

    public static CreatureRecruited event(
            DwellingId dwellingId,
            CreatureId creatureId,
            ArmyId toArmy,
            Amount quantity,
            Resources totalCost
    ) {
        return new CreatureRecruited(
                dwellingId.raw(),
                creatureId.raw(),
                toArmy.raw(),
                quantity.raw(),
                totalCost.raw()
        );
    }

    public static CreatureRecruited event(
            DwellingId dwellingId,
            CreatureId creatureId,
            ArmyId toArmy,
            Amount quantity
    ) {
        return new CreatureRecruited(
                dwellingId.raw(),
                creatureId.raw(),
                toArmy.raw(),
                quantity.raw(),
                Resources.empty().raw()
        );
    }
}
