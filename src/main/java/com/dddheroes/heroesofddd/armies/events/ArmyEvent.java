package com.dddheroes.heroesofddd.armies.events;

public sealed interface ArmyEvent permits CreatureAddedToArmy, CreatureRemovedFromArmy {

    String armyId();
}
