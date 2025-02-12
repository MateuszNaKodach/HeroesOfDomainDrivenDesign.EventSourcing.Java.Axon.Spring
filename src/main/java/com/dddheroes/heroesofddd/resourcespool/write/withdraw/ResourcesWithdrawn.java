package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolEvent;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ResourceType;

public record ResourcesWithdrawn(
        String resourcesPoolId,
        String type,
        Integer amount
) implements ResourcesPoolEvent {

    public static ResourcesWithdrawn event(ResourcesPoolId resourcesPoolId, ResourceType type, Amount amount) {
        return new ResourcesWithdrawn(resourcesPoolId.raw(), type.name(), amount.raw());
    }
}
