package com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingCommand;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.Resources;
import com.dddheroes.heroesofddd.shared.CreatureId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Map;

public record BuildDwelling(
        @TargetAggregateIdentifier
        DwellingId dwellingId,
        CreatureId creatureId,
        Resources costPerTroop
) implements DwellingCommand {

    public static BuildDwelling command(String dwellingId, String creatureId, Map<String, Integer> costPerTroop) {
        return new BuildDwelling(DwellingId.of(dwellingId), CreatureId.of(creatureId), Resources.fromRaw(costPerTroop));
    }
}
