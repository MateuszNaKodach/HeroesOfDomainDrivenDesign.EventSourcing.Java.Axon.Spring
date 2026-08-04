package com.dddheroes.heroesofddd.creaturerecruitment.read.streamdwellings;

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
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates the streaming query type: {@code queryGateway.streamingQuery(...)} returning a
 * reactive {@link org.reactivestreams.Publisher}. The stream is collected into a list to assert on.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StreamDwellingsTest extends DwellingReadModelTest {

    private final QueryGateway queryGateway;

    StreamDwellingsTest(
            AggregateEventPublisher aggregateEventPublisher,
            QueryGateway queryGateway
    ) {
        super(aggregateEventPublisher);
        this.queryGateway = queryGateway;
    }

    @Test
    void givenNoDwellings_whenStreamingQuery_thenEmptyStream() {
        // when
        var query = StreamDwellings.query(GAME_ID);

        // then
        awaitUntilAsserted(() -> assertThat(streamDwellings(query)).isEmpty());
    }

    @Test
    void givenTwoDwellings_whenStreamingQuery_thenBothStreamed() {
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
        var query = StreamDwellings.query(GAME_ID);

        // then
        // This query reads a model projected by a pooled event processor. Under CI load its
        // initial scheduling can outlast the standard integration-test allowance.
        awaitUntilAsserted(Duration.ofSeconds(20), () -> {
            var result = streamDwellings(query);
            assertThat(result).hasSize(2);
            assertThat(result).extracting(DwellingReadModel::getDwellingId)
                              .containsExactlyInAnyOrder(dwellingId1, dwellingId2);
        });
    }

    private List<DwellingReadModel> streamDwellings(StreamDwellings query) {
        return Flux.from(queryGateway.streamingQuery(query, DwellingReadModel.class))
                   .collectList()
                   .block();
    }
}
