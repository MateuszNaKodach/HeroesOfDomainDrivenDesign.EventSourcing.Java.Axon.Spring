package com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;

public record OnlyNotBuiltBuildingCanBeBuild(DwellingId dwellingId) implements DomainRule {

    @Override
    public boolean isViolated() {
        return dwellingId != null;
    }

    @Override
    public String message() {
        return "Only not built building can be build";
    }
}
