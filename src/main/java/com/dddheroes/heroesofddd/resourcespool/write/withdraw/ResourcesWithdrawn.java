package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolEvent;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.Resources;

import java.util.Map;

public record ResourcesWithdrawn(
        String resourcesPoolId,
        Map<String, Integer> resources
) implements ResourcesPoolEvent {

    public static ResourcesWithdrawn event(ResourcesPoolId resourcesPoolId, Resources resources) {
        return new ResourcesWithdrawn(resourcesPoolId.raw(), resources.raw());
    }
}
