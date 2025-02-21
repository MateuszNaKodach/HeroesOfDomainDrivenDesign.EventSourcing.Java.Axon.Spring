package com.dddheroes.heroesofddd.crossed.write.spendresources;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.WithdrawResources;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.modelling.command.Repository;
import org.springframework.stereotype.Component;

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