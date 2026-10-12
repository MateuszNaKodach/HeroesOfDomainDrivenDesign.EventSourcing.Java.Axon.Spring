package com.dddheroes.compatprobes.commandinterop;

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
 * Copy of the application's
 * {@code com.dddheroes.heroesofddd.shared.infrastructure.AxonServerVoidCommandResultWorkaroundConfiguration}
 * interceptor, so the probe can show which mixed-version pairs it repairs. Only the v540workaround variant enables
 * it (through {@code probe.workaround.enabled}), and it only has an effect on the dispatching node: it turns an empty
 * {@code byte[]} command result payload back into a {@code null} payload.
 */
@Configuration
@ConditionalOnProperty(name = "probe.workaround.enabled", havingValue = "true")
class AxonServerVoidCommandResultWorkaroundConfiguration {

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
