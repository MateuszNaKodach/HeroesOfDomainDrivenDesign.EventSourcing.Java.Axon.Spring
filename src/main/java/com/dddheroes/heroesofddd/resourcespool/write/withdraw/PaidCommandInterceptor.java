package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.slices.write.Command;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.modelling.command.Repository;

import javax.annotation.Nonnull;

public class PaidCommandInterceptor implements MessageHandlerInterceptor<CommandMessage<Command>> {

    private final CommandCostResolver commandCostResolver;
    private final Repository<ResourcesPool> resourcesPoolRepository;

    public PaidCommandInterceptor(
            CommandCostResolver commandCostResolver,
            Repository<ResourcesPool> resourcesPoolRepository
    ) {
        this.commandCostResolver = commandCostResolver;
        this.resourcesPoolRepository = resourcesPoolRepository;
    }

    @Override
    public Object handle(
            @Nonnull UnitOfWork<? extends CommandMessage<Command>> unitOfWork,
            @Nonnull InterceptorChain interceptorChain
    ) throws Exception {
        var command = unitOfWork.getMessage();
        var payload = command.getPayload();

        var cost = commandCostResolver.cost(payload);
        var isPaidCommand = cost.isEmpty();
        if (isPaidCommand) {
            var metadata = command.getMetaData();
            var playerId = (String) metadata.get(GameMetaData.PLAYER_ID_KEY);
            withdrawResourcesToSpend(playerId, cost);
        }

        return interceptorChain.proceed();
    }

    private void withdrawResourcesToSpend(String resourcesPoolId, Resources cost) {
        var withdrawResources = WithdrawResources.command(resourcesPoolId, cost.raw());
        resourcesPoolRepository.load(resourcesPoolId)
                               .execute(resourcesPool -> resourcesPool.decide(withdrawResources));
    }
}
