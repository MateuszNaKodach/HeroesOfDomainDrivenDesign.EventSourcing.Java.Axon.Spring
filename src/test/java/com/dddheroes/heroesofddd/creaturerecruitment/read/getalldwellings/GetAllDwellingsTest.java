package com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelTest;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GetAllDwellingsTest extends DwellingReadModelTest {

    private final QueryGateway queryGateway;

    @Autowired
    GetAllDwellingsTest(
            EventGateway eventGateway,
            QueryGateway queryGateway
    ) {
        super(eventGateway);
        this.queryGateway = queryGateway;
    }

    @Test
    void projectingDwellingReadModel_TestCase1() {
        // when
        var query = getAllDwellings();

        // then
        awaitUntilAsserted(() -> {
            var result = getDwellingReadModel(query).dwellings();
            assertThat(result).isEmpty();
        });
    }

    @Test
    void projectingDwellingReadModel_TestCase2() {
        // given
        var creatureId = CreatureIds.phoenix().raw();

        var dwellingId1 = DwellingId.random().raw();
        givenDwellingEvents(
                dwellingId1,
                new DwellingBuilt(dwellingId1, creatureId, PHOENIX_COST)
        );
        var dwellingId2 = DwellingId.random().raw();
        givenDwellingEvents(
                dwellingId2,
                new DwellingBuilt(dwellingId2, creatureId, PHOENIX_COST)
        );

        // when
        var query = getAllDwellings();

        // then
        awaitUntilAsserted(() -> {
            var result = getDwellingReadModel(query).dwellings();
            assertThat(result).hasSize(2);
            assertThat(result).anySatisfy(dwelling -> {
                assertThat(dwelling.getDwellingId()).isEqualTo(dwellingId1);
                assertThat(dwelling.getCreatureId()).isEqualTo(creatureId);
                assertThat(dwelling.getCostPerTroop()).isEqualTo(PHOENIX_COST);
                assertThat(dwelling.getAvailableCreatures()).isEqualTo(0);
            });
            assertThat(result).anySatisfy(dwelling -> {
                assertThat(dwelling.getDwellingId()).isEqualTo(dwellingId2);
                assertThat(dwelling.getCreatureId()).isEqualTo(creatureId);
                assertThat(dwelling.getCostPerTroop()).isEqualTo(PHOENIX_COST);
                assertThat(dwelling.getAvailableCreatures()).isEqualTo(0);
            });
        });
    }

    private static GetAllDwellings getAllDwellings() {
        return GetAllDwellings.query(GAME_ID);
    }

    private GetAllDwellings.Result getDwellingReadModel(GetAllDwellings query) {
        return queryGateway.query(query, GetAllDwellings.Result.class).join();
    }
}