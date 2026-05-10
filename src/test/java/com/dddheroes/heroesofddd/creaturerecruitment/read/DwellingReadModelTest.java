package com.dddheroes.heroesofddd.creaturerecruitment.read;

import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingEvent;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import com.dddheroes.heroesofddd.utils.AggregateEventPublisher;

import java.util.Map;

public abstract class DwellingReadModelTest {

    protected final String GAME_ID = GameId.random().raw();
    protected static final String PLAYER_ID = PlayerId.random().raw();
    protected static final Map<String, Integer> PHOENIX_COST = Map.of(
            ResourceType.GOLD.name(), 2000,
            ResourceType.MERCURY.name(), 1
    );

    protected AggregateEventPublisher aggregateEventPublisher;

    protected DwellingReadModelTest(AggregateEventPublisher aggregateEventPublisher) {
        this.aggregateEventPublisher = aggregateEventPublisher;
    }

    protected void givenDwellingEvents(String dwellingId, DwellingEvent... events) {
        aggregateEventPublisher.publish(
                "Dwelling",
                dwellingId,
                GameMetaData.with(GAME_ID, PLAYER_ID),
                (Object[]) events
        );
    }
}
