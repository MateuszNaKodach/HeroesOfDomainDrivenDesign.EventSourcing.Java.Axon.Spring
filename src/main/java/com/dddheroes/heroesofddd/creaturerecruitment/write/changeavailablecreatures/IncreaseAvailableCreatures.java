package com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingCommand;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record IncreaseAvailableCreatures(
        @TargetAggregateIdentifier
        DwellingId dwellingId,
        CreatureId creatureId,
        Amount increaseBy
) implements DwellingCommand {

    public static IncreaseAvailableCreatures command(String dwellingId, String creatureId, Integer increaseBy) {
        return new IncreaseAvailableCreatures(DwellingId.of(dwellingId),
                                              CreatureId.of(creatureId),
                                              Amount.of(increaseBy));
    }
}
