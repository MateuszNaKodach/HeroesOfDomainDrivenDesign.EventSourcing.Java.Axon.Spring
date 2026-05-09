package com.dddheroes.heroesofddd.resourcespool.events;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.eventhandling.annotation.Event;

import java.util.Map;

@Event
public record ResourcesWithdrawn(
        @EventTag(key = "ResourcesPool")
        String resourcesPoolId,
        Map<String, Integer> resources
) implements ResourcesPoolEvent {

    public static ResourcesWithdrawn event(ResourcesPoolId resourcesPoolId, Resources resources) {
        return new ResourcesWithdrawn(resourcesPoolId.raw(), resources.raw());
    }
}
