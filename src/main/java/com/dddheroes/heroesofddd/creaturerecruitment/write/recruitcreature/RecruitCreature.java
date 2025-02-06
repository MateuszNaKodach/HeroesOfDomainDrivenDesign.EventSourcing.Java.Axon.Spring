package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingCommand;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record RecruitCreature(
        @TargetAggregateIdentifier
        DwellingId dwellingId,
        CreatureId creatureId,
        ArmyId toArmy,
        Amount recruit
) implements DwellingCommand {

}
