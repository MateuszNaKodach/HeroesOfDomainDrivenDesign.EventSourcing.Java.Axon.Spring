package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.gateway.CommandDispatcher;
import org.axonframework.messaging.core.DelayedMessageStream;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageHandlerInterceptor;
import org.axonframework.messaging.core.MessageHandlerInterceptorChain;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class PaidCommandInterceptor implements MessageHandlerInterceptor<CommandMessage> {

    private static final Logger log = LoggerFactory.getLogger(PaidCommandInterceptor.class);

    private final CommandCostResolver commandCostResolver;

    public PaidCommandInterceptor(CommandCostResolver commandCostResolver) {
        this.commandCostResolver = commandCostResolver;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @NonNull MessageStream<?> interceptOnHandle(
            @NonNull CommandMessage message,
            @NonNull ProcessingContext context,
            @NonNull MessageHandlerInterceptorChain<CommandMessage> chain
    ) {
        var cost = commandCostResolver.cost(message, context);
        if (cost.isEmpty()) {
            return chain.proceed(message, context);
        }

        var metadata = message.metadata();
        var playerId = metadata.get(GameMetaData.PLAYER_ID_KEY);
        var playerResourcesPool = ResourcesPoolId.of(playerId);
        // Non-blocking composition: the intercepted command proceeds only once the withdrawal
        // completes; a failed withdrawal fails the delayed stream, rejecting the command.
        var proceedAfterWithdrawal = withdrawResourcesToSpend(playerResourcesPool, cost, context)
                .thenApply(ignored -> {
                    log.info("Player [{}] spent [{}] resources for [{}]", playerId, cost, message.type());
                    return (MessageStream<Message>) chain.proceed(message, context);
                });
        return DelayedMessageStream.create(proceedAfterWithdrawal);
    }

    private CompletableFuture<Void> withdrawResourcesToSpend(
            ResourcesPoolId resourcesPoolId,
            Resources cost,
            ProcessingContext context
    ) {
        var rawResourcesPoolId = resourcesPoolId.raw();
        var withdrawResources = WithdrawResources.command(rawResourcesPoolId, cost.raw());
        return CommandDispatcher.forContext(context).send(withdrawResources).resultAs(Void.class);
    }
}
