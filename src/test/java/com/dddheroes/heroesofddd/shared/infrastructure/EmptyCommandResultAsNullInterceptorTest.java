package com.dddheroes.heroesofddd.shared.infrastructure;

import com.dddheroes.heroesofddd.shared.infrastructure.AxonServerVoidCommandResultWorkaroundConfiguration.EmptyCommandResultAsNullInterceptor;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.CommandResultMessage;
import org.axonframework.messaging.commandhandling.GenericCommandMessage;
import org.axonframework.messaging.commandhandling.GenericCommandResultMessage;
import org.axonframework.messaging.core.Message;
import org.axonframework.messaging.core.MessageStream;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * Pins the behavior of the Axon Server void command result workaround (see
 * AxonServerVoidCommandResultWorkaroundConfiguration and docs/UPGRADE-AXON-5.4.0.md, regression 1).
 * The interceptor must turn only the result the connector produces for a void handler (typed java.lang.Void,
 * empty byte[] data) into a null payload, and leave every other result, including failures and a real empty
 * String result (which Jackson also serializes to an empty byte[]), exactly as it was.
 */
class EmptyCommandResultAsNullInterceptorTest {

    private static final CommandMessage COMMAND =
            new GenericCommandMessage(new MessageType("test.SomeCommand"), "payload");
    private static final MessageType VOID_RESULT_TYPE = new MessageType(Void.class);
    private static final MessageType STRING_RESULT_TYPE = new MessageType(String.class);
    private static final Metadata RESULT_METADATA = Metadata.with("gameId", "game-1").and("playerId", "player-1");

    private final EmptyCommandResultAsNullInterceptor interceptor = new EmptyCommandResultAsNullInterceptor();

    @Nested
    class WhenResultIsAnEmptyVoidResult {

        @Test
        void shouldReplacePayloadWithNull() {
            // given
            var result = voidResultWithPayload(new byte[0]);

            // when
            var intercepted = interceptedResultFor(MessageStream.just(result));

            // then
            assertThat(intercepted.payload()).isNull();
        }

        @Test
        void shouldKeepIdentifierTypeAndMetadata() {
            // given
            var result = voidResultWithPayload(new byte[0]);

            // when
            var intercepted = interceptedResultFor(MessageStream.just(result));

            // then
            assertThat(intercepted).isInstanceOf(CommandResultMessage.class);
            assertThat(intercepted.identifier()).isEqualTo(result.identifier());
            assertThat(intercepted.type()).isEqualTo(VOID_RESULT_TYPE);
            assertThat(intercepted.metadata()).isEqualTo(RESULT_METADATA);
        }
    }

    @Nested
    class WhenResultIsNotAnEmptyVoidResult {

        @Test
        void shouldLeaveEmptyStringResultUntouched() {
            // given
            var result = resultWithPayload(STRING_RESULT_TYPE, new byte[0]);

            // when
            var intercepted = interceptedResultFor(MessageStream.just(result));

            // then
            assertThat(intercepted).isSameAs(result);
            assertThat(intercepted.payload()).isEqualTo(new byte[0]);
        }

        @Test
        void shouldLeaveNonEmptyByteArrayResultUntouched() {
            // given
            var result = resultWithPayload(STRING_RESULT_TYPE, new byte[]{1, 2, 3});

            // when
            var intercepted = interceptedResultFor(MessageStream.just(result));

            // then
            assertThat(intercepted).isSameAs(result);
            assertThat(intercepted.payload()).isEqualTo(new byte[]{1, 2, 3});
        }

        @Test
        void shouldLeaveNonByteArrayResultUntouched() {
            // given
            var result = resultWithPayload(STRING_RESULT_TYPE, "dwelling-1");

            // when
            var intercepted = interceptedResultFor(MessageStream.just(result));

            // then
            assertThat(intercepted).isSameAs(result);
            assertThat(intercepted.payload()).isEqualTo("dwelling-1");
        }

        @Test
        void shouldLeaveAlreadyNullResultUntouched() {
            // given
            var result = voidResultWithPayload(null);

            // when
            var intercepted = interceptedResultFor(MessageStream.just(result));

            // then
            assertThat(intercepted).isSameAs(result);
            assertThat(intercepted.payload()).isNull();
        }
    }

    @Nested
    class WhenResultStreamFailed {

        @Test
        void shouldKeepTheFailure() {
            // given
            var failure = new IllegalStateException("Only not built building can be build");

            // when
            var intercepted = interceptor.interceptOnDispatch(
                    COMMAND, null, (message, context) -> MessageStream.failed(failure)
            );

            // then
            assertThatThrownBy(() -> intercepted.first().asCompletableFuture().join())
                    .isInstanceOf(CompletionException.class)
                    .hasCause(failure);
        }
    }

    private Message interceptedResultFor(MessageStream<? extends Message> resultStream) {
        var intercepted = interceptor.interceptOnDispatch(COMMAND, null, (message, context) -> resultStream);
        var entry = intercepted.first().asCompletableFuture().join();
        assertThat(entry).as("intercepted stream should still carry the result").isNotNull();
        return entry.message();
    }

    private static CommandResultMessage voidResultWithPayload(Object payload) {
        return resultWithPayload(VOID_RESULT_TYPE, payload);
    }

    private static CommandResultMessage resultWithPayload(MessageType type, Object payload) {
        return new GenericCommandResultMessage(type, payload, RESULT_METADATA);
    }
}
