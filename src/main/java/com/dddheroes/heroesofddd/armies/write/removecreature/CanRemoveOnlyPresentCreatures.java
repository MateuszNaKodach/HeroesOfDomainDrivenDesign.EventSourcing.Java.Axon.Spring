package com.dddheroes.heroesofddd.armies.write.removecreature;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;

import java.util.Map;

public record CanRemoveOnlyPresentCreatures(
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
        return "Can remove only present creatures";
    }
}
