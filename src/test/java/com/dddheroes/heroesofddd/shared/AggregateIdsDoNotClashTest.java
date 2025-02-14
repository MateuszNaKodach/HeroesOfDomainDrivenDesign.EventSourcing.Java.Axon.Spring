package com.dddheroes.heroesofddd.shared;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersId;
import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
public class AggregateIdsDoNotClashTest {

    @Autowired
    private EventStore eventStore;

    @Test
    void givenSameIdValueForDifferentAggregateTypes_WhenStoreEvent_ThenDoNotClash() {
        // given
        var rawId = UUID.randomUUID().toString();
        var differentAggregateTypeIds = Map.of(
                "Army", ArmyId.of(rawId),
                "Astrologers", AstrologersId.of(rawId),
                "Calendar", CalendarId.of(rawId),
                "Dwelling", DwellingId.of(rawId),
                "ResourcesPool", ResourcesPoolId.of(rawId)
        );

        // when/then
        assertDoesNotThrow(() -> {
            differentAggregateTypeIds.forEach((aggregateType, aggregateId) -> {
                storeAggregateEvent(aggregateType, aggregateId);
            });
        });
    }

    private void storeAggregateEvent(String aggregateType, Object aggregateId) {
        eventStore.publish(domainEvent(aggregateType, aggregateId.toString(), "payload"));
    }


    private static DomainEventMessage<?> domainEvent(
            String aggregateType,
            String identifier,
            Object payload
    ) {
        return new GenericDomainEventMessage<>(
                aggregateType,
                identifier,
                0,
                payload
        );
    }
}
