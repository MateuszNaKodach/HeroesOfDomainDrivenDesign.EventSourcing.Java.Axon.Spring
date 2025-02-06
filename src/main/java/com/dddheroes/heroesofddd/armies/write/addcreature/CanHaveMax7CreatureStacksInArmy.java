package com.dddheroes.heroesofddd.armies.write.addcreature;

import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.CreatureId;
import com.dddheroes.heroesofddd.shared.DomainRule;

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
