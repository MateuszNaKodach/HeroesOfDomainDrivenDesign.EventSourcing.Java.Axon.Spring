package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.slices.write.Command;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.core.MessageHandlerInterceptor;
import org.axonframework.messaging.core.MessageHandlerInterceptorChain;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.modelling.repository.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaidCommandInterceptor implements MessageHandlerInterceptor<CommandMessage> {

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
    public MessageStream<?> interceptOnHandle(CommandMessage message, ProcessingContext context, MessageHandlerInterceptorChain<CommandMessage> chain) {
        if (message.payload() instanceof Command payload) {
            var cost = commandCostResolver.cost(payload);
            var isPaidCommand = !cost.isEmpty();
            if (isPaidCommand) {
                var metadata = message.metadata();
                var playerId = (String) metadata.get(GameMetaData.PLAYER_ID_KEY);
                var playerResourcesPool = ResourcesPoolId.of(playerId);
                withdrawResourcesToSpend(playerResourcesPool, cost);
                log.info("Player [{}] spent [{}] resources for [{}]", playerId, cost, payload);
            }
        }

        return chain.proceed();
    }

    private void withdrawResourcesToSpend(ResourcesPoolId resourcesPoolId, Resources cost) {
        var rawResourcesPoolId = resourcesPoolId.raw();
        var withdrawResources = WithdrawResources.command(rawResourcesPoolId, cost.raw());
        resourcesPoolRepository.loadOrCreate(rawResourcesPoolId, () -> new ResourcesPool(resourcesPoolId))
                               .execute(resourcesPool -> resourcesPool.decide(withdrawResources));
    }
}
