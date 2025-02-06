package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.DomainRule;

public record RecruitCreaturesNotExceedAvailableCreatures(
        Amount availableCreatures,
        Amount recruit
) implements DomainRule {

    @Override
    public boolean isViolated() {
        return recruit.compareTo(availableCreatures) > 0;
    }

    @Override
    public String message() {
        return "Recruit creatures not exceed available creatures";
    }
}
