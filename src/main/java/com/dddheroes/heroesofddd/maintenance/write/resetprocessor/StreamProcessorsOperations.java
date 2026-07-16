package com.dddheroes.heroesofddd.maintenance.write.resetprocessor;

import org.axonframework.common.configuration.AxonConfiguration;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.messaging.eventhandling.processing.streaming.StreamingEventProcessor;
import org.axonframework.messaging.eventhandling.processing.streaming.segmenting.EventTrackerStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class StreamProcessorsOperations {

    private final AxonConfiguration axonConfiguration;
    private final EventStorageEngine eventStorageEngine;

    StreamProcessorsOperations(AxonConfiguration axonConfiguration, EventStorageEngine eventStorageEngine) {
        this.axonConfiguration = axonConfiguration;
        this.eventStorageEngine = eventStorageEngine;
    }

    public CompletableFuture<Void> reset(String processor) {
        return streamingProcessor(processor)
                .filter(StreamingEventProcessor::supportsReset)
                .map(eventProcessor -> eventProcessor.shutdown().orTimeout(30, TimeUnit.SECONDS)
                                                     .thenCompose(v -> eventProcessor.resetTokens()
                                                                                     .orTimeout(30, TimeUnit.SECONDS))
                                                     .thenCompose(v -> eventProcessor.start()
                                                                                     .orTimeout(30, TimeUnit.SECONDS)))
                .orElseGet(() -> CompletableFuture.completedFuture(null));
    }

    public CompletableFuture<Optional<Progress>> progressOf(String processor) {
        var maybeProcessor = streamingProcessor(processor);
        if (maybeProcessor.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        var eventProcessor = maybeProcessor.get();
        // Non-blocking: segment positions come from the in-memory processingStatus() (instead of the
        // async TokenStore, which requires an active ProcessingContext), the stream's head from the
        // storage engine's latestToken().
        return eventStorageEngine.latestToken().thenApply(headToken -> {
            var tail = headToken == null ? OptionalLong.empty() : headToken.position();
            var current = eventProcessor.processingStatus().values().stream()
                                        .map(EventTrackerStatus::getCurrentPosition)
                                        .filter(OptionalLong::isPresent)
                                        .mapToLong(OptionalLong::getAsLong)
                                        .min();
            if (current.isEmpty() || tail.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new Progress(current.getAsLong(), tail.getAsLong()));
        });
    }

    private Optional<StreamingEventProcessor> streamingProcessor(String processor) {
        return axonConfiguration
                .getModuleConfiguration("EventProcessor[" + processor + "]")
                .flatMap(m -> m.getOptionalComponent(StreamingEventProcessor.class));
    }

    public record Progress(long current, long tail) {

        public BigDecimal progress() {
            return BigDecimal.valueOf(current, 2)
                             .divide(BigDecimal.valueOf(tail, 2), RoundingMode.HALF_UP);
        }
    }
}
