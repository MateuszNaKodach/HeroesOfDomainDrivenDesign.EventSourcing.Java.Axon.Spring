package com.dddheroes.heroesofddd.maintenance.write.resetprocessor;

import org.axonframework.common.configuration.AxonConfiguration;
import org.axonframework.messaging.eventhandling.processing.streaming.StreamingEventProcessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class StreamProcessorsOperations {

    private final AxonConfiguration axonConfiguration;

    StreamProcessorsOperations(AxonConfiguration axonConfiguration) {
        this.axonConfiguration = axonConfiguration;
    }

    public void reset(String processor) {
        axonConfiguration
                .getModuleConfiguration("EventProcessor[" + processor + "]")
                .flatMap(m -> m.getOptionalComponent(StreamingEventProcessor.class))
                .ifPresent(eventProcessor -> {
                    if (eventProcessor.supportsReset()) {
                        eventProcessor.shutdown().orTimeout(30, TimeUnit.SECONDS).join();
                        eventProcessor.resetTokens().orTimeout(30, TimeUnit.SECONDS).join();
                        eventProcessor.start().orTimeout(30, TimeUnit.SECONDS).join();
                    }
                });
    }

    @Transactional
    public Optional<Progress> progressOf(String processor) {
        // TODO #LLM: AF5 migration deferred — AF5 TokenStore.fetchSegments / fetchToken
        // are async (CompletableFuture) and require a ProcessingContext, which is not
        // available outside an active unit-of-work. Reimplement via
        // StreamingEventProcessor.processingStatus() (per-segment EventTrackerStatus)
        // looked up via axonConfiguration.getModuleConfiguration("EventProcessor[" +
        // processor + "]").flatMap(m -> m.getOptionalComponent(StreamingEventProcessor.class)).
        throw new UnsupportedOperationException(
                "progressOf is deferred during AF4→AF5 migration; reimplement using StreamingEventProcessor.processingStatus()");
    }

    public record Progress(long current, long tail) {

        public BigDecimal progress() {
            return BigDecimal.valueOf(current, 2)
                             .divide(BigDecimal.valueOf(tail, 2), RoundingMode.HALF_UP);
        }
    }
}
