package com.dddheroes.heroesofddd.creaturerecruitment.events;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.eventhandling.annotation.Event;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.eventhandling.annotation.Event;

@Event
public record AvailableCreaturesChanged(
        @EventTag(key = "Dwelling")
        String dwellingId,
        String creatureId,
        Integer changedTo
) implements DwellingEvent {

    public static AvailableCreaturesChanged event(DwellingId dwellingId, CreatureId creatureId, Amount changedTo) {
        return new AvailableCreaturesChanged(dwellingId.raw(), creatureId.raw(), changedTo.raw());
    }
}
