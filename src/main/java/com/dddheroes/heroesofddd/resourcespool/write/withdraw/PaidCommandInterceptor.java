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

import jakarta.annotation.Nonnull;

// TODO #LLM: migrate the body of this interceptor to the AF5 API — the signature has been rewritten but the body still references the AF4 `unitOfWork` / `interceptorChain` / `messages` parameters. Replace those with calls on `message`, `context`, `chain`. See docs/reference-guide/modules/migration/pages/paths/interceptors.adoc
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
        var command = unitOfWork.getMessage();

        if (command.payload() instanceof Command payload) {
            var cost = commandCostResolver.cost(payload);
            var isPaidCommand = !cost.isEmpty();
            if (isPaidCommand) {
                var metadata = command.metadata();
                var playerId = (String) metadata.get(GameMetaData.PLAYER_ID_KEY);
                var playerResourcesPool = ResourcesPoolId.of(playerId);
                withdrawResourcesToSpend(playerResourcesPool, cost);
                log.info("Player [{}] spent [{}] resources for [{}]", playerId, cost, payload);
            }
        }

        return interceptorChain.proceed();
    }

    private void withdrawResourcesToSpend(ResourcesPoolId resourcesPoolId, Resources cost) throws Exception {
        var rawResourcesPoolId = resourcesPoolId.raw();
        var withdrawResources = WithdrawResources.command(rawResourcesPoolId, cost.raw());
        resourcesPoolRepository.loadOrCreate(rawResourcesPoolId, () -> new ResourcesPool(resourcesPoolId))
                               .execute(resourcesPool -> resourcesPool.decide(withdrawResources));
    }
}
