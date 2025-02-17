package com.dddheroes.heroesofddd.shared.payments;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.WithdrawResources;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.modelling.command.Repository;
import org.springframework.stereotype.Component;

class PaymentInterceptor implements MessageHandlerInterceptor<CommandMessage<?>> {

    private final Repository<ResourcesPool> repository;

    PaymentInterceptor(Repository<ResourcesPool> repository) {
        this.repository = repository;
    }

    @Override
    public Object handle(UnitOfWork<? extends CommandMessage<?>> unitOfWork, InterceptorChain interceptorChain) throws Exception {
        CommandMessage<?> command = unitOfWork.getMessage();

        if (command.getPayload() instanceof PaidCommand paidCommand) {
            var resourcesPoolId = paidCommand.resourcesPoolId().raw();
            var cost = paidCommand.cost();
            var gold = cost.raw().getOrDefault("GOLD", 0);

            repository.load(resourcesPoolId)
                                   .execute(resourcesPool -> resourcesPool.decide(
                                           WithdrawResources.command(resourcesPoolId, "GOLD", gold))
                                   );
        }

        return interceptorChain.proceed();
    }
}