package com.dddheroes.heroesofddd.resourcespool.write.deposit;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolEvent;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.Resources;

import java.util.Map;

public record ResourcesDeposited(
        String resourcesPoolId,
        Map<String, Integer> resources
) implements ResourcesPoolEvent {

    public static ResourcesDeposited event(ResourcesPoolId resourcesPoolId, Resources resources) {
        return new ResourcesDeposited(resourcesPoolId.raw(), resources.raw());
    }
}
