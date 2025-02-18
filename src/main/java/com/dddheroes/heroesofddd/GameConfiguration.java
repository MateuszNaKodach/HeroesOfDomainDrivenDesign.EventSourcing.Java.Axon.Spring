package com.dddheroes.heroesofddd;

import com.dddheroes.heroesofddd.shared.GameMetaData;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.async.SequencingPolicy;
import org.axonframework.messaging.correlation.CorrelationDataProvider;
import org.axonframework.messaging.correlation.MessageOriginProvider;
import org.axonframework.messaging.correlation.SimpleCorrelationDataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameConfiguration {

    @Bean
    public SequencingPolicy<EventMessage<?>> gameIdSequencingPolicy() {
        return e -> e.getMetaData().get(GameMetaData.GAME_ID_KEY);
    }

    @Bean //todo: check if not overwriting the default correlation data provider
    public CorrelationDataProvider gameDataProvider() {
        return new SimpleCorrelationDataProvider(GameMetaData.GAME_ID_KEY, GameMetaData.PLAYER_ID_KEY);
    }

    @Bean
    public CorrelationDataProvider messageOriginProvider() {
        return new MessageOriginProvider();
    }
}
