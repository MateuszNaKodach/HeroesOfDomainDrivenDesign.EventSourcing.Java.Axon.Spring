package com.dddheroes.heroesofddd.resourcespool.write.deposit;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolEvent;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ResourceType;

public record ResourcesDeposited(
        String resourcesPoolId,
        String type,
        Integer amount
) implements ResourcesPoolEvent {

    public static ResourcesDeposited event(ResourcesPoolId resourcesPoolId, ResourceType type, Amount amount) {
        return new ResourcesDeposited(resourcesPoolId.raw(), type.name(), amount.raw());
    }
}
