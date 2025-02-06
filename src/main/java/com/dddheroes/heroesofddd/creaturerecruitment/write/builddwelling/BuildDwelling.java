package com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingCommand;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.Cost;
import com.dddheroes.heroesofddd.shared.CreatureId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Map;

public record BuildDwelling(
        @TargetAggregateIdentifier
        DwellingId dwellingId,
        CreatureId creatureId,
        Cost costPerTroop
) implements DwellingCommand {

    public static BuildDwelling command(String dwellingId, String creatureId, Map<String, Integer> costPerTroop) {
        return new BuildDwelling(DwellingId.of(dwellingId), CreatureId.of(creatureId), Cost.fromRaw(costPerTroop));
    }
}
