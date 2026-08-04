package com.dddheroes.heroesofddd.creaturerecruitment.read.listdwellings;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelTest;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.utils.AggregateEventPublisher;
import org.axonframework.messaging.queryhandling.gateway.QueryGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates the multi-result query type: {@code queryGateway.queryMany(...)} returning a
 * {@code CompletableFuture<List<DwellingReadModel>>} from a handler that returns a {@link List}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ListDwellingsTest extends DwellingReadModelTest {

    private final QueryGateway queryGateway;

    ListDwellingsTest(
            AggregateEventPublisher aggregateEventPublisher,
            QueryGateway queryGateway
    ) {
        super(aggregateEventPublisher);
        this.queryGateway = queryGateway;
    }

    @Test
    void givenNoDwellings_whenQueryMany_thenEmptyList() {
        // when
        var query = ListDwellings.query(GAME_ID);

        // then
        awaitUntilAsserted(() -> assertThat(listDwellings(query)).isEmpty());
    }

    @Test
    void givenTwoDwellings_whenQueryMany_thenBothReturned() {
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
        var query = ListDwellings.query(GAME_ID);

        // then
        awaitUntilAsserted(() -> {
            var result = listDwellings(query);
            assertThat(result).hasSize(2);
            assertThat(result).extracting(DwellingReadModel::getDwellingId)
                              .containsExactlyInAnyOrder(dwellingId1, dwellingId2);
        });
    }

    private List<DwellingReadModel> listDwellings(ListDwellings query) {
        return queryGateway.queryMany(query, DwellingReadModel.class).join();
    }
}
