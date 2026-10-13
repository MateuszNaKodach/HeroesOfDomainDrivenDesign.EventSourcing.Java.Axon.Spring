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
 * Workaround for a regression in Axon Framework / Axoniq Framework since 5.3.2 (still in 5.4.0): a command handler
 * without a result (a {@code void} handler), reached through Axon Server, produces a result that the dispatcher can
 * no longer read as {@code Void}.
 * <p>
 * Since Axon Framework commit {@code faaff07d36} (PR #4994, "Ensure returned CommandResultMessage is never null",
 * backported to 5.3.x as {@code 4b971815a1}, PR #4999) a void handler returns a result message typed
 * {@code java.lang.Void} with a {@code null} payload instead of no result message. The handling side of
 * {@code io.axoniq.framework.axonserver.connector.command.CommandConverter#convertResultMessage} sends that as a
 * payload typed {@code java.lang.Void} with an empty {@code byte[]}. The dispatching side
 * ({@code CommandConverter#convertCommandResponse}) only treats a response without a payload type as "no result",
 * so the empty {@code byte[]} becomes the result payload. {@code CommandResult#resultAs(Void.class)} then hands it
 * to Jackson, which fails with "No content to map due to end-of-input". The local 5.3.0-SNAPSHOT build this app ran
 * on before did not have that change, so it was not affected.
 * <p>
 * This dispatch interceptor turns exactly that result (typed {@code java.lang.Void}, empty {@code byte[]}) back into
 * a {@code null} payload, so all {@code resultAs(Void.class)} call sites keep working unchanged. Other empty results,
 * such as an empty {@code String} that Jackson also serializes to an empty {@code byte[]}, are left as they are.
 * Remove this class once a framework release reads void results correctly.
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

        private static final String VOID_RESULT_TYPE_NAME = Void.class.getName();

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
            if (VOID_RESULT_TYPE_NAME.equals(result.type().name())
                    && result.payload() instanceof byte[] bytes && bytes.length == 0) {
                return new GenericCommandResultMessage(
                        new GenericMessage(result.identifier(), result.type(), null, result.metadata())
                );
            }
            return result;
        }
    }
}
