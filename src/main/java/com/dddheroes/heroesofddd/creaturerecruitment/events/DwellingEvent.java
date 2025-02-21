package com.dddheroes.heroesofddd.creaturerecruitment.events;

public sealed interface DwellingEvent permits DwellingBuilt, AvailableCreaturesChanged, CreatureRecruited {

    String dwellingId();
}
