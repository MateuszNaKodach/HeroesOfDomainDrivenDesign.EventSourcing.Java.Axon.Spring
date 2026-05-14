package com.dddheroes.heroesofddd.resourcespool.events;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.eventhandling.annotation.Event;

import java.util.Map;

@Event
public record ResourcesDeposited(
        @EventTag(key = "ResourcesPool")
        String resourcesPoolId,
        Map<String, Integer> resources
) implements ResourcesPoolEvent {

    public static ResourcesDeposited event(ResourcesPoolId resourcesPoolId, Resources resources) {
        return new ResourcesDeposited(resourcesPoolId.raw(), resources.raw());
    }
}
