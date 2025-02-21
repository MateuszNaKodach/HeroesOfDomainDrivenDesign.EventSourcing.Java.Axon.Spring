package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.shared.domain.DomainRule;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;

public record CannotWithdrawMoreThanDepositedResources(Resources balance, Resources toWithdraw) implements DomainRule {

    @Override
    public boolean isViolated() {
        return !balance.contains(toWithdraw);
    }

    @Override
    public String message() {
        return "Cannot withdraw more than deposited resources";
    }
}
