package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.DomainRule;

public record CannotWithdrawMoreThanDepositedResources(DwellingId dwellingId) implements DomainRule {

    @Override
    public boolean isViolated() {
        return dwellingId != null;
    }

    @Override
    public String message() {
        return "Cannot withdraw more than deposited resources";
    }
}
