package com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingCommand;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.CreatureId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record IncreaseAvailableCreatures(
        @TargetAggregateIdentifier
        DwellingId dwellingId,
        CreatureId creatureId,
        Amount increaseBy
) implements DwellingCommand {

}
