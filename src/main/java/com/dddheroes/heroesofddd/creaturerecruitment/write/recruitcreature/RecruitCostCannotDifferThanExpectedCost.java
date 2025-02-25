package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.shared.domain.DomainRule;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;

public record RecruitCostCannotDifferThanExpectedCost(
        Resources costPerTroop,
        Resources expectedCostPerTroop
) implements DomainRule {

    @Override
    public boolean isViolated() {
        return !costPerTroop.isSame(expectedCostPerTroop);
    }

    @Override
    public String message() {
        return "Recruit cost cannot differ than expected cost";
    }
}
