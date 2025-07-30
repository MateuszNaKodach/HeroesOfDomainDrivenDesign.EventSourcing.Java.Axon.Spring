package com.dddheroes.heroesofddd;

import org.axonframework.axonserver.connector.TargetContextResolver;
import org.axonframework.messaging.Message;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class CommandBusConfiguration {

    @Primary
    @Bean
    @ConditionalOnProperty("axon.axonserver.enabled")
    public TargetContextResolver<Message<?>> targetContextResolver() {
        return message -> message.getPayloadType().getName().startsWith("com.context.booking") ? "booking" : "payment";
    }

}
