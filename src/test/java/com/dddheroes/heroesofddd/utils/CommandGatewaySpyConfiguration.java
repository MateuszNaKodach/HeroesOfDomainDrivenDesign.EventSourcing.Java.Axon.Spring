package com.dddheroes.heroesofddd.utils;

import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/*
 * Wraps the AF5-registered `commandGateway` Spring bean with a Mockito spy so
 * automation tests can `verify(commandGateway).send(...)` on it.
 *
 * Why a BeanPostProcessor instead of `@MockitoSpyBean`:
 * Spring Boot's `@MockitoSpyBean` (`BeanOverrideContextCustomizer`) runs as a
 * `BeanFactoryPostProcessor`. It needs the spied type to already have a
 * `BeanDefinition` at that phase. AF5's `SpringComponentRegistry` registers
 * framework components (CommandGateway, EventGateway, QueryGateway, …) as
 * Spring `BeanDefinition`s only lazily, from `postProcessAfterInitialization`
 * during bean instantiation — strictly after the BFPP phase. The two never
 * line up, so `@MockitoSpyBean CommandGateway` aborts the refresh with
 * "Unable to select a bean to wrap".
 *
 * A user `@Bean CommandGateway` that delegates to `axonConfiguration
 * .getComponent(...)` cannot work either: AF5's `SpringConfiguration
 * .getComponent(type)` calls `beanFactory.getBean(type)`, which resolves back
 * to the user `@Bean` (currently in creation) → BeanCurrentlyInCreation.
 *
 * Instead, we wrap at instantiation time. By then AF5 has already produced
 * the real (decorated) `CommandGateway` instance via its component supplier;
 * we replace the singleton-cache entry with `Mockito.spy(real)`. Subsequent
 * lookups (the automation processors via AF5's Configuration → Spring) get
 * the spy, so tests can verify against it through plain `@Autowired
 * CommandGateway`.
 */
@TestConfiguration
public class CommandGatewaySpyConfiguration {

    @Bean
    static BeanPostProcessor commandGatewaySpyPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof CommandGateway gateway && CommandGateway.class.getName().equals(beanName)) {
                    return Mockito.spy(gateway);
                }
                return bean;
            }
        };
    }
}
