package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.slices.write.Command;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.messaging.InterceptorChain;
import org.axonframework.messaging.MessageHandlerInterceptor;
import org.axonframework.messaging.unitofwork.UnitOfWork;
import org.axonframework.modelling.command.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class PaidCommandInterceptor implements MessageHandlerInterceptor<CommandMessage<?>> {

    private static final Logger log = LoggerFactory.getLogger(PaidCommandInterceptor.class);

    private final CommandCostResolver<Command> commandCostResolver;
    private final Repository<ResourcesPool> resourcesPoolRepository;

    public PaidCommandInterceptor(
            CommandCostResolver<Command> commandCostResolver,
            Repository<ResourcesPool> resourcesPoolRepository
    ) {
        this.commandCostResolver = commandCostResolver;
        this.resourcesPoolRepository = resourcesPoolRepository;
    }

    @Override
    public Object handle(
            @Nonnull UnitOfWork<? extends CommandMessage<?>> unitOfWork,
            @Nonnull InterceptorChain interceptorChain
    ) throws Exception {
        var command = unitOfWork.getMessage();

        if (command.getPayload() instanceof Command payload) {
            var cost = commandCostResolver.cost(payload);
            var isPaidCommand = !cost.isEmpty();
            if (isPaidCommand) {
                var metadata = command.getMetaData();
                var playerId = (String) metadata.get(GameMetaData.PLAYER_ID_KEY);
                var playerResourcesPool = ResourcesPoolId.of(playerId);
                withdrawResourcesToSpend(playerResourcesPool, cost);
                log.info("Player [{}] spent [{}] resources for [{}]", playerId, cost, payload);
            }
        }

        return interceptorChain.proceed();
    }

    private void withdrawResourcesToSpend(ResourcesPoolId resourcesPoolId, Resources cost) {
        var rawResourcesPoolId = resourcesPoolId.raw();
        var withdrawResources = WithdrawResources.command(rawResourcesPoolId, cost.raw());
        resourcesPoolRepository.load(rawResourcesPoolId)
                               .execute(resourcesPool -> resourcesPool.decide(withdrawResources));
    }
}
