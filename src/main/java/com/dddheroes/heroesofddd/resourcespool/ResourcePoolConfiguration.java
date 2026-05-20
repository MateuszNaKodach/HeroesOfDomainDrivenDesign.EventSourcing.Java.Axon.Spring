package com.dddheroes.heroesofddd.resourcespool;

import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.resourcespool.application.ComposedCommandCostResolver;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.PaidCommandInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
class ResourcePoolConfiguration {

    @ConditionalOnProperty(value = "application.interceptors.paid-commands.enabled", havingValue = "true")
    @Bean
    PaidCommandInterceptor paidCommandInterceptor(Set<CommandCostResolver<?>> commandCostResolvers) {
        return new PaidCommandInterceptor(new ComposedCommandCostResolver(commandCostResolvers));
    }
}
