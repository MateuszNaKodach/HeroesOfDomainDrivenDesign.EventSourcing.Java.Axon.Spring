package com.dddheroes.heroesofddd.resourcespool;

import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.resourcespool.application.ComposedCommandCostResolver;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.PaidCommandInterceptor;
import org.axonframework.modelling.command.Repository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
class ResourcePoolConfiguration {

    @Bean
    PaidCommandInterceptor paidCommandInterceptor(
            Set<CommandCostResolver> commandCostResolvers,
            Repository<ResourcesPool> resourcesPoolRepository
    ) {
        return new PaidCommandInterceptor(
                new ComposedCommandCostResolver(commandCostResolvers),
                resourcesPoolRepository
        );
    }
}
