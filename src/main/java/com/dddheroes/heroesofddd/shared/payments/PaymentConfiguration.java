package com.dddheroes.heroesofddd.shared.payments;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool;
import org.axonframework.commandhandling.CommandBus;
import org.axonframework.modelling.command.Repository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PaymentConfiguration implements InitializingBean {
    private final CommandBus commandBus;
    private final Repository<ResourcesPool> repository;

    public PaymentConfiguration(CommandBus commandBus, Repository<ResourcesPool> repository) {
        this.commandBus = commandBus;
        this.repository = repository;
    }

    @Override
    public void afterPropertiesSet() {
        commandBus.registerHandlerInterceptor(new PaymentInterceptor(repository));
    }
}