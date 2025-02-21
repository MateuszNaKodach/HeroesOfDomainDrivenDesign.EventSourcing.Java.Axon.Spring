package com.dddheroes.heroesofddd.armies.write;

import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.slices.write.Command;

public interface ArmyCommand extends Command {

    ArmyId armyId();
}
