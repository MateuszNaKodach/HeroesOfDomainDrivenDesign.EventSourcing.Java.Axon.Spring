package com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingEvent;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.CreatureRecruited;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.shared.GameId;
import com.dddheroes.heroesofddd.shared.GameMetaData;
import com.dddheroes.heroesofddd.shared.ResourceType;
import com.dddheroes.heroesofddd.utils.AwaitilityUtils;
import org.awaitility.Awaitility;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.*;
import static org.assertj.core.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GetDwellingByIdTest {

    private static final String GAME_ID = GameId.random().raw();
    private static final Map<String, Integer> PHOENIX_COST = Map.of(
            ResourceType.GOLD.name(), 2000,
            ResourceType.MERCURY.name(), 1
    );

    @Autowired
    private QueryGateway queryGateway;

    // AxonServer event store is EventBus, but what about other implementations like JPA? Should I use EventStore instead?
    @Autowired
    private EventGateway eventGateway;

    @Test
    void projectingDwellingReadModel_TestCase1() {
        // given
        var dwellingId = DwellingId.random().raw();

        // when
        var query = getDwellingById(dwellingId);

        // then
        awaitUntilAsserted(() -> {
            var result = getDwellingReadModel(query);
            assertThat(result).isNull();
        });
    }

    @Test
    void projectingDwellingReadModel_TestCase2() {
        // given
        var dwellingId = DwellingId.random().raw();
        var creatureId = CreatureIds.phoenix().raw();
        givenDwellingEvents(
                dwellingId,
                new DwellingBuilt(dwellingId, creatureId, PHOENIX_COST)
        );

        // when
        var query = getDwellingById(dwellingId);

        // then
        awaitUntilAsserted(() -> {
            var result = getDwellingReadModel(query);
            assertThat(result).isNotNull();
            assertThat(result.getDwellingId()).isEqualTo(dwellingId);
            assertThat(result.getCreatureId()).isEqualTo(creatureId);
            assertThat(result.getCostPerTroop()).isEqualTo(PHOENIX_COST);
            assertThat(result.getAvailableCreatures()).isEqualTo(0);
        });
    }

    @Test
    void projectingDwellingReadModel_TestCase3() {
        // given
        var dwellingId = DwellingId.random().raw();
        var creatureId = CreatureIds.phoenix().raw();
        givenDwellingEvents(
                dwellingId,
                new DwellingBuilt(dwellingId, creatureId, PHOENIX_COST),
                new AvailableCreaturesChanged(dwellingId, creatureId, 3)
        );

        // when
        var query = getDwellingById(dwellingId);

        // then
        awaitUntilAsserted(() -> {
            var result = getDwellingReadModel(query);
            assertThat(result).isNotNull();
            assertThat(result.getDwellingId()).isEqualTo(dwellingId);
            assertThat(result.getCreatureId()).isEqualTo(creatureId);
            assertThat(result.getCostPerTroop()).isEqualTo(PHOENIX_COST);
            assertThat(result.getAvailableCreatures()).isEqualTo(3);
        });
    }

    @Test
    void projectingDwellingReadModel_TestCase4() {
        // given
        var dwellingId = DwellingId.random().raw();
        var creatureId = CreatureIds.phoenix().raw();
        givenDwellingEvents(
                dwellingId,
                new DwellingBuilt(dwellingId, creatureId, PHOENIX_COST),
                new AvailableCreaturesChanged(dwellingId, creatureId, 3),
                new CreatureRecruited(dwellingId, creatureId, ArmyId.random().raw(), 1, PHOENIX_COST)
        );

        // when
        var query = getDwellingById(dwellingId);

        // then
        awaitUntilAsserted(() -> {
            var result = getDwellingReadModel(query);
            assertThat(result).isNotNull();
            assertThat(result.getDwellingId()).isEqualTo(dwellingId);
            assertThat(result.getCreatureId()).isEqualTo(creatureId);
            assertThat(result.getCostPerTroop()).isEqualTo(PHOENIX_COST);
            assertThat(result.getAvailableCreatures()).isEqualTo(2);
        });
    }

    private static GetDwellingById getDwellingById(String dwellingId) {
        return GetDwellingById.query(GAME_ID, dwellingId);
    }

    private DwellingReadModel getDwellingReadModel(GetDwellingById query) {
        return queryGateway.query(query, DwellingReadModel.class).join();
    }

    private void givenDwellingEvents(String dwellingId, DwellingEvent... events) {
        for (int i = 0; i < events.length; i++) {
            eventGateway.publish(dwellingDomainEvent(dwellingId, i, events[i]));
        }
    }

    private static DomainEventMessage<?> dwellingDomainEvent(String dwellingId, int sequenceNumber, DwellingEvent payload) {
        return new GenericDomainEventMessage<>(
                "Dwelling",
                dwellingId,
                sequenceNumber,
                payload
        ).andMetaData(GameMetaData.withId(GAME_ID));
    }
}