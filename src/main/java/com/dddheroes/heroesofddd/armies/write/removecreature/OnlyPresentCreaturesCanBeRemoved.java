package com.dddheroes.heroesofddd.armies.write.removecreature;

import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.CreatureId;
import com.dddheroes.heroesofddd.shared.DomainRule;

import java.util.Map;

public record OnlyPresentCreaturesCanBeRemoved(
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
        return "Cannot add more than 7 different creature stacks to the army";
    }
}
