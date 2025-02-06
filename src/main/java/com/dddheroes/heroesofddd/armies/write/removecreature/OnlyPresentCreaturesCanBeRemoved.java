package com.dddheroes.heroesofddd.armies.write.removecreature;

import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.CreatureId;
import com.dddheroes.heroesofddd.shared.DomainRule;

import java.util.Map;

public record OnlyPresentCreaturesCanBeRemoved(
        CreatureId creatureToRemove,
        Amount quantityToRemove,
        Map<CreatureId, Amount> creatureStacksBeforeRemoved
) implements DomainRule {

    @Override
    public boolean isViolated() {
        return creatureStacksBeforeRemoved
                .getOrDefault(creatureToRemove, Amount.zero()).raw() < quantityToRemove.raw();
    }

    @Override
    public String message() {
        return "Only present creatures can be removed";
    }
}
