package com.dddheroes.heroesofddd.shared.payments;

import jakarta.annotation.PostConstruct;
import org.axonframework.commandhandling.CommandBus;
import org.springframework.context.annotation.Configuration;

@Configuration
class PaymentConfiguration {

//    @Autowired
//    void configure(CommandBus commandBus, PaymentInterceptor paymentInterceptor) {
//        commandBus.registerHandlerInterceptor(paymentInterceptor);
//    }


    private final CommandBus commandBus;
    private final org.axonframework.config.Configuration configuration;

    public PaymentConfiguration(CommandBus commandBus, org.axonframework.config.Configuration configuration) {
        this.commandBus = commandBus;
        this.configuration = configuration;
    }

    @PostConstruct
    void paymentInterceptor() {
        commandBus
                .registerHandlerInterceptor(new PaymentInterceptor(configuration));
    }
}