package com.dddheroes.heroesofddd.utils;

import org.axonframework.messaging.core.LegacyResources;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.unitofwork.UnitOfWorkFactory;
import org.axonframework.messaging.eventhandling.EventMessage;
import org.axonframework.messaging.eventhandling.EventSink;
import org.axonframework.messaging.eventhandling.GenericEventMessage;
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
 *     AggregateBasedJpaEventStorageEngine.mapToEntry; the AggregateSequencer auto-allocates
 *     aggregate_sequence_number from 0 when AppendCondition.none() is in effect.
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
 * The utility resolves the EventSink from the ProcessingContext rather than @Autowiring it,
 * because AF5 framework components are registered as Spring beans lazily by SpringComponentRegistry
 * (during postProcessAfterInitialization) and ctx.component(EventSink.class) hits the AF5
 * component registry directly without the timing issue.
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

                    List<EventMessage> messages = payloads.stream()
                            .map(payload -> (EventMessage) new GenericEventMessage(
                                    new MessageType(payload.getClass()),
                                    payload,
                                    metadata))
                            .toList();
                    return ctx.component(EventSink.class).publish(ctx, messages);
                })
                .join();
    }
}
