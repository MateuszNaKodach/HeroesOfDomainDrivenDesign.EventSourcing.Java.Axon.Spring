package com.dddheroes.heroesofddd.shared.infrastructure;

import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.core.GenericMessage;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageDispatchInterceptor;
import org.axonframework.messaging.core.MessageDispatchInterceptorChain;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Workaround for a regression in the Axon Server connector of Axoniq Framework 5.4.0: a command handler without a
 * result (a {@code void} handler) produces a result that can no longer be read as {@code Void}.
 * <p>
 * Since commit {@code f2b25ad6a9} ("Drop nullability for CommandResultMessage") the handling side of
 * {@code io.axoniq.framework.axonserver.connector.command.CommandConverter#convertResultMessage} sends a
 * {@code null} result as a payload with the result type name and an empty {@code byte[]}. The dispatching side
 * ({@code CommandConverter#convertCommandResponse}) only treats a response without a payload type as "no result",
 * so the empty {@code byte[]} becomes the result payload. {@code CommandResult#resultAs(Void.class)} then hands it
 * to Jackson, which fails with "No content to map due to end-of-input".
 * <p>
 * This dispatch interceptor restores the 5.3 behavior by turning an empty {@code byte[]} result payload back into
 * a {@code null} payload, so all {@code resultAs(Void.class)} call sites keep working unchanged. Commands of this
 * application never return an empty byte array as a real result. Remove this class once the connector is fixed.
 * <p>
 * Set {@code application.workarounds.axon-server-void-command-result.enabled=false} to switch the workaround off,
 * for example to check whether a newer connector version still needs it.
 */
@Configuration
@ConditionalOnProperty(prefix = "axon.axonserver", name = "enabled", havingValue = "true")
class AxonServerVoidCommandResultWorkaroundConfiguration {

    @ConditionalOnProperty(prefix = "application.workarounds.axon-server-void-command-result", name = "enabled",
            havingValue = "true", matchIfMissing = true)
    @Bean
    MessageDispatchInterceptor<CommandMessage> emptyCommandResultAsNullInterceptor() {
        return new EmptyCommandResultAsNullInterceptor();
    }

    static class EmptyCommandResultAsNullInterceptor implements MessageDispatchInterceptor<CommandMessage> {

        @Override
        public @NonNull MessageStream<?> interceptOnDispatch(
                @NonNull CommandMessage message,
                @Nullable ProcessingContext context,
                @NonNull MessageDispatchInterceptorChain<CommandMessage> chain
        ) {
            return chain.proceed(message, context)
                        .mapMessage(EmptyCommandResultAsNullInterceptor::nullForEmptyPayload);
        }

        private static Message nullForEmptyPayload(Message result) {
            if (result.payload() instanceof byte[] bytes && bytes.length == 0) {
                return new GenericCommandResultMessage(
                        new GenericMessage(result.identifier(), result.type(), null, result.metadata())
                );
            }
            return result;
        }
    }
}
