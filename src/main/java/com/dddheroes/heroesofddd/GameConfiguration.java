package com.dddheroes.heroesofddd;

import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import io.axoniq.framework.springboot.autoconfig.OpenTelemetryTracingAutoConfiguration;
import org.axonframework.messaging.core.correlation.CorrelationDataProvider;
import org.axonframework.messaging.core.correlation.MessageOriginProvider;
import org.axonframework.messaging.core.correlation.SimpleCorrelationDataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameConfiguration {

    @Bean
    public CorrelationDataProvider gameDataProvider() {
        return new SimpleCorrelationDataProvider(GameMetaData.GAME_ID_KEY, GameMetaData.PLAYER_ID_KEY);
    }

    @Bean
    public CorrelationDataProvider messageOriginProvider() {
        return new MessageOriginProvider();
    }
}
