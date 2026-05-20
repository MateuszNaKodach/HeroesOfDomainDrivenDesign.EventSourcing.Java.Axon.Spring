package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.gateway.CommandDispatcher;
import org.axonframework.messaging.core.MessageHandlerInterceptor;
import org.axonframework.messaging.core.MessageHandlerInterceptorChain;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaidCommandInterceptor implements MessageHandlerInterceptor<CommandMessage> {

    private static final Logger log = LoggerFactory.getLogger(PaidCommandInterceptor.class);

    private final CommandCostResolver commandCostResolver;

    public PaidCommandInterceptor(CommandCostResolver commandCostResolver) {
        this.commandCostResolver = commandCostResolver;
    }

    @Override
    public @NonNull MessageStream<?> interceptOnHandle(
            @NonNull CommandMessage message,
            @NonNull ProcessingContext context,
            @NonNull MessageHandlerInterceptorChain<CommandMessage> chain
    ) {
        var cost = commandCostResolver.cost(message, context);
        if (!cost.isEmpty()) {
            var metadata = message.metadata();
            var playerId = metadata.get(GameMetaData.PLAYER_ID_KEY);
            var playerResourcesPool = ResourcesPoolId.of(playerId);
            withdrawResourcesToSpend(playerResourcesPool, cost, context);
            log.info("Player [{}] spent [{}] resources for [{}]", playerId, cost, message.type());
        }

        return chain.proceed(message, context);
    }

    private void withdrawResourcesToSpend(ResourcesPoolId resourcesPoolId, Resources cost, ProcessingContext context) {
        var rawResourcesPoolId = resourcesPoolId.raw();
        var withdrawResources = WithdrawResources.command(rawResourcesPoolId, cost.raw());
        CommandDispatcher.forContext(context).send(withdrawResources).resultAs(Void.class).join();
    }
}
