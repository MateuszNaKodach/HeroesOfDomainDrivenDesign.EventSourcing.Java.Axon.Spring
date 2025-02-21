package com.dddheroes.heroesofddd.resourcespool.write.spend;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.WithdrawResources;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.modelling.command.Repository;
import org.springframework.stereotype.Component;

/**
 * It handles {@link WithdrawResources} and another command {@link SpendResources#buy} in the single UnitOfWork.
 *
 * It's a cross-cutting component, and by this interceptor couple two Slices in the single UnitOfWork and the event
 * store transaction. You may use Automations/Saga/Process Manager for "Resources Transaction" to coordinate your "paid
 * commands" with the {@link SpendResources} and introduce some compensating actions, but with low risk of contention -
 * a user spent his own resources it's more pragmatic way to go.
 * <p>
 * The approach may change with Dynamic Consistency Boundary in place.
 */
@Component
class SpendResourcesCommandInterceptor implements MessageHandlerInterceptor<CommandMessage<?>> {

    private final Repository<ResourcesPool> resourcesPoolRepository;

    SpendResourcesCommandInterceptor(Repository<ResourcesPool> resourcesPoolRepository) {
        this.resourcesPoolRepository = resourcesPoolRepository;
    }

    @Override
    public Object handle(
            UnitOfWork<? extends CommandMessage<?>> unitOfWork,
            InterceptorChain interceptorChain
    ) throws Exception {
        CommandMessage<?> command = unitOfWork.getMessage();
        if (command.getPayload() instanceof SpendResources spendResources) {
            withdrawResourcesToSpend(spendResources);
            var buy = spendResources.buy();
            unitOfWork.transformMessage(c -> new GenericCommandMessage<>(buy, command.getMetaData()));
        }

        return interceptorChain.proceed();
    }

    private void withdrawResourcesToSpend(SpendResources spendResources) {
        var resourcesPoolId = spendResources.resourcesPoolId().raw();
        var resources = spendResources.resources();

        var withdrawResources = WithdrawResources.command(resourcesPoolId, resources.raw());
        resourcesPoolRepository.load(resourcesPoolId)
                               .execute(resourcesPool -> resourcesPool.decide(withdrawResources));
    }
}