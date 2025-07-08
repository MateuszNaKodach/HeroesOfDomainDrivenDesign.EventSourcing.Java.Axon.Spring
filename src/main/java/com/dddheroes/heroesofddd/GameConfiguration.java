package com.dddheroes.heroesofddd;

import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.infrastructure.serialization.DwellingIdSerializationModule;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.async.SequencingPolicy;
import org.axonframework.messaging.correlation.CorrelationDataProvider;
import org.axonframework.messaging.correlation.MessageOriginProvider;
import org.axonframework.messaging.correlation.SimpleCorrelationDataProvider;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.json.JacksonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class GameConfiguration {

    @Bean
    public SequencingPolicy<EventMessage<?>> gameIdSequencingPolicy() {
        return e -> e.getMetaData().get(GameMetaData.GAME_ID_KEY);
    }

    @Bean
    public CorrelationDataProvider gameDataProvider() {
        return new SimpleCorrelationDataProvider(GameMetaData.GAME_ID_KEY, GameMetaData.PLAYER_ID_KEY);
    }

    @Bean
    public CorrelationDataProvider messageOriginProvider() {
        return new MessageOriginProvider();
    }

    @Bean
    public Module dwellingIdSerializationModule() {
        return new DwellingIdSerializationModule();
    }

//    @Bean
//    @Primary
//    public Serializer defaultSerializer(ObjectMapper objectMapper) {
//        objectMapper.registerModule(dwellingIdSerializationModule());
//        return JacksonSerializer.builder()
//                .objectMapper(objectMapper)
//                .build();
//    }
}
