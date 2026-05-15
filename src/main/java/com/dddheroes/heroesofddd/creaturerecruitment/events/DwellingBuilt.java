package com.dddheroes.heroesofddd.creaturerecruitment.events;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.eventhandling.annotation.Event;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.eventhandling.annotation.Event;

import java.util.Map;

@Event
public record DwellingBuilt(
        @EventTag(key = "Dwelling")
        String dwellingId,
        String creatureId,
        Map<String, Integer> costPerTroop
) implements DwellingEvent {

    public static DwellingBuilt event(DwellingId dwellingId, CreatureId creatureId, Resources costPerTroop) {
        return new DwellingBuilt(dwellingId.raw(), creatureId.raw(), costPerTroop.raw());
    }
}
