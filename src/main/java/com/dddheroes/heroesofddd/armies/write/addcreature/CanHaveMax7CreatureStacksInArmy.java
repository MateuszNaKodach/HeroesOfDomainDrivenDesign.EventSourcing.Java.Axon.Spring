package com.dddheroes.heroesofddd.armies.write.addcreature;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;

import java.util.Map;

public record CanHaveMax7CreatureStacksInArmy(
        CreatureId creatureToAdd,
        Map<CreatureId, Amount> creatureStacksBeforeAdd
) implements DomainRule {

    private static final int MAX_CREATURE_STACKS = 7;

    @Override
    public boolean isViolated() {
        return creatureStacksBeforeAdd.size() >= MAX_CREATURE_STACKS
                && !creatureStacksBeforeAdd.containsKey(creatureToAdd);
    }

    @Override
    public String message() {
        return "Can have max 7 different creature stacks in the army";
    }
}
