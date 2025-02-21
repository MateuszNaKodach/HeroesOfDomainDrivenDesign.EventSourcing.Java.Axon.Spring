package com.dddheroes.heroesofddd.creaturerecruitment.events;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;

import java.util.Map;

public record CreatureRecruited(
        String dwellingId,
        String creatureId,
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
}
