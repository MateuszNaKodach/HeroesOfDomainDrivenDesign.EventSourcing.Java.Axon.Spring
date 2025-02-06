package com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import org.awaitility.Awaitility;
import org.awaitility.core.AssertionCondition;
import org.awaitility.core.Condition;
import org.awaitility.core.ThrowingRunnable;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GetDwellingByIdTest {

    @Autowired
    private QueryGateway queryGateway;

    @Autowired
    private EventGateway eventGateway;

    @Autowired
    private EventStore eventStore;

    @Test
    void dwellingNotExist() {
        // when
        var result = queryGateway.query(GetDwellingById.query("1"), DwellingReadModel.class).join();

        // then
        assertThat(result).isNull();
    }

    @Test
    void dwellingExist() {
        // given
//        eventGateway.publish(new DwellingBuilt("2", "2", new HashMap<>()));
        eventStore.publish(new GenericDomainEventMessage<>(
                "Dwelling",
                "2",
                0,
                new DwellingBuilt("2", "2", new HashMap<>())
        ));

        // when
        var query = GetDwellingById.query("2");

        // then
        awaitUntilAsserted(() -> {
            var result = queryGateway.query(GetDwellingById.query("2"), DwellingReadModel.class).join();
            assertThat(result).isNotNull();
        });
    }

    private void dwellingDomainEvent(String dwellingId, int sequenceNumber, Object payload) {
        eventGateway.publish(new GenericDomainEventMessage<>(
                "Dwelling",
                dwellingId,
                sequenceNumber,
                new DwellingBuilt(dwellingId, dwellingId, new HashMap<>())
        ));
    }

    private void awaitUntilAsserted(Runnable assertion) {
        Awaitility.await()
                  .pollInSameThread()
                  .atMost(Duration.ofSeconds(5))
                  .untilAsserted(assertion::run);
    }
}