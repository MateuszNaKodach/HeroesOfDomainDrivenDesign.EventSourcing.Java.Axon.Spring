package com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;

public record OnlyBuiltDwellingCanHaveAvailableCreatures(DwellingId dwellingId) implements DomainRule {

    @Override
    public boolean isViolated() {
        return dwellingId == null;
    }

    @Override
    public String message() {
        return "Only built dwelling can have available creatures";
    }
}
