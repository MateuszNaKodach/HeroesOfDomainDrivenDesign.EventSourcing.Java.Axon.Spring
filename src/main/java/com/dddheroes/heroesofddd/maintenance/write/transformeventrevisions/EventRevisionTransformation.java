package com.dddheroes.heroesofddd.maintenance.write.transformeventrevisions;

import io.axoniq.axonserver.connector.AxonServerConnection;
import io.axoniq.axonserver.connector.AxonServerConnectionFactory;
import io.axoniq.axonserver.connector.event.EventStream;
import io.axoniq.axonserver.connector.event.transformation.ActiveTransformation;
import io.axoniq.axonserver.connector.event.transformation.Appender;
import io.axoniq.axonserver.connector.event.transformation.EventTransformation;
import io.axoniq.axonserver.connector.event.transformation.EventTransformationChannel;
import io.axoniq.axonserver.connector.event.transformation.event.EventSources;
import io.axoniq.axonserver.connector.impl.ServerAddress;
import io.axoniq.axonserver.grpc.SerializedObject;
import io.axoniq.axonserver.grpc.control.NodeInfo;
import io.axoniq.axonserver.grpc.event.Event;
import io.axoniq.axonserver.grpc.event.EventWithToken;
import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Maintenance operation that rewrites legacy events whose payload revision is empty, giving them an explicit revision,
 * via the Axon Server <a
 * href="https://docs.axoniq.io/axon-server-reference/v2026.0/axon-server/administration/event-transformation/">Event
 * Transformation</a> API.
 * <p>
 * Events stored with Axon Framework 4 may not carry a revision (Axon Server stores it as an empty {@code String}).
 * Reading such events under Axon Framework 5 fails with {@code "The given version is unsupported because it is empty."}
 * (AxonIQ/AxonFramework#4625). Running this <b>before</b> migrating to AF5 fills in the revision so the events read
 * cleanly afterwards.
 * <p>
 * It opens its <b>own dedicated</b> {@link AxonServerConnection} (separate from the application's managed connection),
 * runs the transformation, and closes it again.
 * <p>
 * <b>Important:</b> transformation rewrites the event store in place (back up first), transforms <b>events only</b>
 * (not snapshots), needs the {@code TRANSFORM}/{@code TRANSFORM_ADMIN} roles, and only one transformation may be active
 * per context at a time.
 */
@Component
@ConditionalOnExpression("${application.maintenance.enabled:false} and ${axon.axonserver.enabled:false}")
public class EventRevisionTransformation {

    private static final Logger logger = LoggerFactory.getLogger(EventRevisionTransformation.class);

    private static final long STEP_TIMEOUT_SECONDS = 60;
    private static final long READ_TIMEOUT_SECONDS = 15;
    private static final int APPLY_POLL_ATTEMPTS = 600;
    private static final long APPLY_POLL_INTERVAL_MILLIS = 500;

    private final AxonServerConfiguration axonServerConfiguration;

    EventRevisionTransformation(AxonServerConfiguration axonServerConfiguration) {
        this.axonServerConfiguration = axonServerConfiguration;
    }

    /**
     * Replaces every event with an empty payload revision so that it carries the given {@code revision}, and applies
     * the transformation.
     *
     * @param revision the revision to assign to events that currently have none (e.g. {@code "0.0.1"})
     * @param compact  when {@code true}, compacts the store after applying to reclaim the disk used by the
     *                 pre-transformation segment versions
     * @return a summary of the transformation
     */
    public Result fillMissingRevision(String revision, boolean compact) {
        NodeInfo server = axonServerConfiguration.routingServers().get(0);
        String context = axonServerConfiguration.getContext();
        AxonServerConnectionFactory factory =
                AxonServerConnectionFactory.forClient(axonServerConfiguration.getComponentName() + "-event-transformer")
                                           .routingServers(new ServerAddress(server.getHostName(), server.getGrpcPort()))
                                           .build();
        AxonServerConnection connection = factory.connect(context);
        try {
            EventTransformationChannel channel = connection.eventTransformationChannel();

            // Only one ACTIVE transformation is allowed per context; clear a lingering one from a previous staged or
            // failed run before starting a new one.
            cancelActiveTransformations(channel);

            long lastToken = await(connection.eventChannel().getLastToken());
            if (lastToken < 0) {
                return new Result(null, "EMPTY_STORE", 0, false);
            }

            // Collect the events to fix with a TERMINATING reader. EventSources.range(...) reads from a *live* event
            // stream that blocks forever once it reaches the end of a quiet store (it waits for the next token that
            // never arrives), so we read up to the last token ourselves and then feed a finite
            // EventSources.fromIterable(...), which completes instead of hanging.
            List<EventWithToken> eventsWithoutRevision = readEventsWithoutRevision(connection, lastToken);

            logger.info("Found {} event(s) without a revision up to token {} on context '{}' (compact={})",
                        eventsWithoutRevision.size(), lastToken, context, compact);

            if (eventsWithoutRevision.isEmpty()) {
                return new Result(null, "NOTHING_TO_FIX", 0, false);
            }

            AtomicLong replaced = new AtomicLong();
            EventTransformation created = await(
                    EventSources.fromIterable(eventsWithoutRevision)
                                .transform(
                                        "Fill missing payload revision with '" + revision + "' (#4625)",
                                        (EventWithToken eventWithToken, Appender appender) -> {
                                            Event original = eventWithToken.getEvent();
                                            SerializedObject payload = original.getPayload()
                                                                               .toBuilder()
                                                                               .setRevision(revision)
                                                                               .build();
                                            // toBuilder() preserves identifier, aggregate id/type/sequence, timestamp
                                            // and metadata - only the payload revision changes.
                                            appender.replaceEvent(eventWithToken.getToken(),
                                                                  original.toBuilder().setPayload(payload).build());
                                            replaced.incrementAndGet();
                                        })
                                .execute(connection::eventTransformationChannel)
            );

            logger.info("Transformation {} submitted with {} replacement(s) (state={})",
                        created.id(), replaced.get(), created.state());

            // execute() submits the replacements; on this connector the transformation then applies automatically.
            // If it is still ACTIVE, start applying it explicitly. Either way, wait until it reaches APPLIED.
            if (created.state() == EventTransformation.State.ACTIVE) {
                await(channel.activeTransformation().thenCompose(ActiveTransformation::startApplying));
            }
            EventTransformation applied = awaitState(channel, created.id(), EventTransformation.State.APPLIED);
            logger.info("Transformation {} applied ({} event(s) replaced)", applied.id(), replaced.get());

            if (compact) {
                await(channel.startCompacting());
                logger.info("Compaction started for the event store");
            }

            return new Result(applied.id(), applied.state().name(), replaced.get(), compact);
        } finally {
            if (connection.isConnected()) {
                connection.disconnect();
            }
            factory.shutdown();
        }
    }

    private List<EventWithToken> readEventsWithoutRevision(AxonServerConnection connection, long lastToken) {
        List<EventWithToken> result = new ArrayList<>();
        try (EventStream stream = connection.eventChannel().openStream(-1L, 100)) {
            EventWithToken eventWithToken;
            while ((eventWithToken = stream.nextIfAvailable(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)) != null) {
                if (eventWithToken.getToken() > lastToken) {
                    break;
                }
                if (eventWithToken.getEvent().getPayload().getRevision().isEmpty()) {
                    result.add(eventWithToken);
                }
                if (eventWithToken.getToken() >= lastToken) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading events from the event store", e);
        }
        return result;
    }

    private void cancelActiveTransformations(EventTransformationChannel channel) {
        for (EventTransformation transformation : await(channel.transformations())) {
            if (transformation.state() == EventTransformation.State.ACTIVE) {
                logger.warn("Cancelling a pre-existing ACTIVE transformation {} ('{}') before starting a new one",
                            transformation.id(), transformation.description());
                await(channel.activeTransformation().thenCompose(ActiveTransformation::cancel));
            }
        }
    }

    private EventTransformation awaitState(EventTransformationChannel channel,
                                           String transformationId,
                                           EventTransformation.State target) {
        for (int attempt = 0; attempt < APPLY_POLL_ATTEMPTS; attempt++) {
            EventTransformation transformation = find(channel, transformationId);
            if (transformation.state() == target) {
                return transformation;
            }
            if (transformation.state() == EventTransformation.State.CANCELLED) {
                throw new IllegalStateException("Transformation " + transformationId + " was cancelled");
            }
            sleep();
        }
        throw new IllegalStateException(
                "Transformation " + transformationId + " did not reach " + target + " within the expected time");
    }

    private EventTransformation find(EventTransformationChannel channel, String transformationId) {
        for (EventTransformation transformation : await(channel.transformations())) {
            if (transformation.id().equals(transformationId)) {
                return transformation;
            }
        }
        throw new IllegalStateException("Transformation " + transformationId + " not found");
    }

    private static <T> T await(CompletableFuture<T> future) {
        try {
            return future.get(STEP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for an event transformation step", e);
        } catch (Exception e) {
            throw new IllegalStateException("Event transformation step failed: " + e.getMessage(), e);
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(APPLY_POLL_INTERVAL_MILLIS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the transformation to be applied", e);
        }
    }

    /**
     * Summary of a triggered transformation.
     *
     * @param transformationId the Axon Server transformation id
     * @param state            the transformation state (e.g. {@code ACTIVE}, {@code APPLIED})
     * @param eventsReplaced   the number of events whose revision was filled in
     * @param compacted        whether the store was compacted afterwards
     */
    public record Result(String transformationId, String state, long eventsReplaced, boolean compacted) {

    }
}
