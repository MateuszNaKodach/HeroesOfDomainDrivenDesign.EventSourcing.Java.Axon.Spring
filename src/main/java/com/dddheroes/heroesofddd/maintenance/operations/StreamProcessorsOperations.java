package com.dddheroes.heroesofddd.maintenance.operations;

import org.axonframework.config.EventProcessingConfiguration;
import org.axonframework.eventhandling.ReplayToken;
import org.axonframework.eventhandling.TrackingEventProcessor;
import org.axonframework.eventhandling.tokenstore.TokenStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.IntStream;

@Component
public class StreamProcessorsOperations {

    private final EventProcessingConfiguration eventProcessingConfiguration;
    private final TokenStore tokenStore;

    StreamProcessorsOperations(EventProcessingConfiguration eventProcessingConfiguration, TokenStore tokenStore) {
        this.eventProcessingConfiguration = eventProcessingConfiguration;
        this.tokenStore = tokenStore;
    }

    public void reset(String processor) {
        eventProcessingConfiguration
                .eventProcessorByProcessingGroup(processor, TrackingEventProcessor.class)
                .ifPresent(eventProcessor -> {
                    if (eventProcessor.supportsReset()) {
                        eventProcessor.shutDown();
                        eventProcessor.resetTokens();
                        eventProcessor.start();
                    }
                });
    }

    @Transactional
    public Optional<Progress> progressOf(String processor) {
        var segments = tokenStore.fetchSegments(processor);

        if (segments.length == 0) {
            return Optional.empty();
        } else {
            var accumulatedProgress = IntStream.of(segments).mapToObj(segment -> {
                var token = tokenStore.fetchToken(processor, segment);

                var maybeCurrent = token.position();
                var maybePositionAtReset = token instanceof ReplayToken replayToken
                        ? replayToken.getTokenAtReset().position()
                        : OptionalLong.empty();

                return new Progress(maybeCurrent.orElse(0L), maybePositionAtReset.orElse(0L));
            }).reduce(new Progress(0, 0), (acc, progress) ->
                    new Progress(acc.current + progress.current, acc.tail + progress.tail));

            return (accumulatedProgress.tail == 0L) ? Optional.empty() : Optional.of(accumulatedProgress);
        }
    }

    public record Progress(long current, long tail) {

        public BigDecimal progress() {
            return BigDecimal.valueOf(current, 2)
                             .divide(BigDecimal.valueOf(tail, 2), RoundingMode.HALF_UP);
        }
    }
}
