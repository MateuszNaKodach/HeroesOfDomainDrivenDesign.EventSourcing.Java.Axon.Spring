package com.dddheroes.heroesofddd.utils;

import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.eventsourcing.eventstore.EventStoreTransaction;
import org.axonframework.eventsourcing.eventstore.SourcingCondition;
import org.axonframework.messaging.core.LegacyResources;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.unitofwork.UnitOfWorkFactory;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
import org.axonframework.messaging.eventstreaming.EventCriteria;
import org.axonframework.messaging.eventstreaming.Tag;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/*
 * Test-side replacement for AF4's eventGateway.publish(GenericDomainEventMessage(...)) pattern.
 *
 * AF4 attached aggregate-routing data (aggregateType, aggregateIdentifier, sequenceNumber) inline
 * on the DomainEventMessage envelope. AF5 splits that responsibility:
 *
 *   - aggregate identifier / aggregate type on the WRITE path are read from @EventTag on the
 *     event payload by AnnotationBasedTagResolver and consumed by
 *     AggregateBasedJpaEventStorageEngine.mapToEntry; the AggregateSequencer allocates each new
 *     aggregate_sequence_number based on the AppendCondition's consistency marker. With an
 *     AppendCondition of `none()`, allocation starts at 0 — so two publish() calls for the same
 *     aggregate_identifier collide on the unique (aggregate_identifier, aggregate_sequence_number)
 *     index.
 *
 *   - publishing has to happen inside an active ProcessingContext so events flow through
 *     DefaultEventStoreTransaction and commit atomically. We obtain one from the framework's
 *     UnitOfWorkFactory.
 *
 *   - the legacy DomainEventMessage envelope fields are now exposed as LegacyResources keys on
 *     the event Context. AggregateBasedJpaEventStorageEngine.buildContext sets them on the read
 *     side; we mirror them on the write side so any handler/parameter resolver that reads them
 *     via LegacyResources sees the same values AF4 would have set on the message envelope.
 *
 * To support multiple publish() calls per aggregate, we first source the aggregate's existing
 * stream within the same ProcessingContext. DefaultEventStoreTransaction.source() captures the
 * stream's ConsistencyMarker into its internal appendPositionKey resource, and
 * attachAppendEventsStep() then builds an AppendCondition that includes that marker, so
 * AggregateSequencer.incrementAndGetSequenceOf(...) continues from the existing position rather
 * than restarting at 0. An empty aggregate still works — the marker has no entry for that
 * identifier and the sequencer falls back to 0.
 *
 * Tag key for the SourcingCondition is the same key used by the application's @EventTag /
 * AnnotationBasedTagResolver on the event payloads, i.e. the aggregate type name. This must match
 * how the production aggregates are tagged (Dwelling, Army, Calendar, Astrologers, …).
 */
@Component
public class AggregateEventPublisher {

    private final UnitOfWorkFactory unitOfWorkFactory;

    AggregateEventPublisher(UnitOfWorkFactory unitOfWorkFactory) {
        this.unitOfWorkFactory = unitOfWorkFactory;
    }

    public void publish(String aggregateType, String aggregateId, Metadata metadata, Object... payloads) {
        publish(aggregateType, aggregateId, metadata, Arrays.asList(payloads));
    }

    public void publish(String aggregateType, String aggregateId, Metadata metadata, List<?> payloads) {
        unitOfWorkFactory.create()
                .executeWithResult(ctx -> {
                    ctx.putResource(LegacyResources.AGGREGATE_TYPE_KEY, aggregateType);
                    ctx.putResource(LegacyResources.AGGREGATE_IDENTIFIER_KEY, aggregateId);

                    EventStoreTransaction transaction = ctx.component(EventStore.class).transaction(ctx);

                    SourcingCondition existing = SourcingCondition.conditionFor(
                            EventCriteria.havingTags(Tag.of(aggregateType, aggregateId))
                    );
                    return transaction.source(existing).ignoreEntries().asCompletableFuture()
                            .thenAccept(__ -> payloads.stream()
                                    .map(payload -> (EventMessage) new GenericEventMessage(
                                            new MessageType(payload.getClass()),
                                            payload,
                                            metadata))
                                    .forEach(transaction::appendEvent));
                })
                .join();
    }
}
