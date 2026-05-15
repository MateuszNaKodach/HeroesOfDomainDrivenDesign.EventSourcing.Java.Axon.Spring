package com.dddheroes.heroesofddd.maintenance.write.resetprocessor;

import org.axonframework.messaging.eventhandling.processing.streaming.StreamingEventProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Component
public class StreamProcessorsOperations {

    private final ApplicationContext applicationContext;

    StreamProcessorsOperations(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void reset(String processor) {
        applicationContext.getBeansOfType(StreamingEventProcessor.class).values().stream()
                .filter(ep -> ep.name().equals(processor))
                .findFirst()
                .ifPresent(eventProcessor -> {
                    if (eventProcessor.supportsReset()) {
                        eventProcessor.shutdown().join();
                        eventProcessor.resetTokens().join();
                        eventProcessor.start().join();
                    }
                });
    }

    public Optional<Progress> progressOf(String processor) {
        return applicationContext.getBeansOfType(StreamingEventProcessor.class).values().stream()
                .filter(ep -> ep.name().equals(processor))
                .findFirst()
                .flatMap(eventProcessor -> {
                    var statusMap = eventProcessor.processingStatus();
                    if (statusMap.isEmpty()) {
                        return Optional.empty();
                    }
                    var accumulated = statusMap.values().stream()
                            .reduce(new Progress(0, 0),
                                    (acc, status) -> new Progress(
                                            acc.current() + status.getCurrentPosition().orElse(0L),
                                            acc.tail() + status.getResetPosition().orElse(0L)
                                    ),
                                    (a, b) -> new Progress(a.current() + b.current(), a.tail() + b.tail()));
                    return accumulated.tail() == 0L ? Optional.empty() : Optional.of(accumulated);
                });
    }

    public record Progress(long current, long tail) {

        public BigDecimal progress() {
            return BigDecimal.valueOf(current, 2)
                             .divide(BigDecimal.valueOf(tail, 2), RoundingMode.HALF_UP);
        }
    }
}
