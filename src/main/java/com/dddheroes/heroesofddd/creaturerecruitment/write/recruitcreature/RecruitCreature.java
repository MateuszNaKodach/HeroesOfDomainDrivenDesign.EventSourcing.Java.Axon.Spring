package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingCommand;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Map;

public record RecruitCreature(
        @TargetAggregateIdentifier
        DwellingId dwellingId,
        CreatureId creatureId,
        ArmyId toArmy,
        Amount quantity,
        Resources expectedCost
) implements DwellingCommand {

    public static RecruitCreature command(
            String dwellingId,
            String creatureId,
            String toArmy,
            Integer quantity,
            Map<String, Integer> expectedCost
    ) {
        return new RecruitCreature(
                DwellingId.of(dwellingId),
                CreatureId.of(creatureId),
                ArmyId.of(toArmy),
                Amount.of(quantity),
                Resources.fromRaw(expectedCost)
        );
    }
}
