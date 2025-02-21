package com.dddheroes.heroesofddd.creaturerecruitment.read;

import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingEvent;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.gateway.EventGateway;

import java.util.Map;

public abstract class DwellingReadModelTest {

    protected final String GAME_ID = GameId.random().raw();
    protected static final String PLAYER_ID = PlayerId.random().raw();
    protected static final Map<String, Integer> PHOENIX_COST = Map.of(
            ResourceType.GOLD.name(), 2000,
            ResourceType.MERCURY.name(), 1
    );

    protected EventGateway eventGateway;

    protected DwellingReadModelTest(EventGateway eventGateway) {
        this.eventGateway = eventGateway;
    }

    protected void givenDwellingEvents(String dwellingId, DwellingEvent... events) {
        for (int i = 0; i < events.length; i++) {
            eventGateway.publish(dwellingDomainEvent(dwellingId, i, events[i]));
        }
    }

    protected DomainEventMessage<?> dwellingDomainEvent(String dwellingId, int sequenceNumber,
                                                             DwellingEvent payload) {
        return new GenericDomainEventMessage<>(
                "Dwelling",
                dwellingId,
                sequenceNumber,
                payload
        ).andMetaData(GameMetaData.with(GAME_ID, PLAYER_ID));
    }
}
