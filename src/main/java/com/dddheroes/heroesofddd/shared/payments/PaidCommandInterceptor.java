package com.dddheroes.heroesofddd.shared.payments;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.WithdrawResources;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.config.Configuration;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.modelling.command.Repository;
import org.springframework.stereotype.Component;

class PaymentInterceptor implements MessageHandlerInterceptor<CommandMessage<?>> {

    private final Configuration configuration;

    PaymentInterceptor(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Object handle(UnitOfWork<? extends CommandMessage<?>> unitOfWork, InterceptorChain interceptorChain) throws Exception {
        CommandMessage<?> command = unitOfWork.getMessage();

        if (command.getPayload() instanceof PaidCommand paidCommand) {
            var resourcesPoolId = paidCommand.resourcesPoolId().raw();
            var cost = paidCommand.cost();
            var gold = cost.raw().getOrDefault("GOLD", 0);

            configuration.repository(ResourcesPool.class).load(resourcesPoolId)
                                   .execute(resourcesPool -> resourcesPool.decide(
                                           WithdrawResources.command(resourcesPoolId, "GOLD", gold))
                                   );
        }

        return interceptorChain.proceed();
    }
}