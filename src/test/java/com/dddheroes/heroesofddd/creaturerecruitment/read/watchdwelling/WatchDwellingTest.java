package com.dddheroes.heroesofddd.creaturerecruitment.read.watchdwelling;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelTest;
import com.dddheroes.heroesofddd.creaturerecruitment.events.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.utils.AggregateEventPublisher;
import org.axonframework.messaging.queryhandling.gateway.QueryGateway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.concurrent.CopyOnWriteArrayList;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates the subscription query type: {@code queryGateway.subscriptionQuery(...)} returning a
 * reactive {@link org.reactivestreams.Publisher} that emits the initial read-model state followed by
 * live updates. Updates are emitted by {@code DwellingReadModelProjector} whenever the dwelling changes.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class WatchDwellingTest extends DwellingReadModelTest {

    private final QueryGateway queryGateway;

    WatchDwellingTest(
            AggregateEventPublisher aggregateEventPublisher,
            QueryGateway queryGateway
    ) {
        super(aggregateEventPublisher);
        this.queryGateway = queryGateway;
    }

    @Test
    void givenBuiltDwelling_whenSubscribing_thenReceivesInitialStateAndLiveUpdate() {
        // given
        var dwellingId = DwellingId.random().raw();
        var creatureId = CreatureIds.phoenix().raw();
        givenDwellingEvents(
                dwellingId,
                new DwellingBuilt(dwellingId, creatureId, PHOENIX_COST)
        );
        // wait until the read model is projected, so the subscription's initial result is present
        awaitUntilAsserted(() -> assertThat(currentState(dwellingId)).isNotNull());

        // when
        var received = new CopyOnWriteArrayList<DwellingReadModel>();
        Disposable subscription = Flux.from(
                queryGateway.subscriptionQuery(WatchDwelling.query(GAME_ID, dwellingId), DwellingReadModel.class)
        ).subscribe(received::add);

        try {
            // the initial result (availableCreatures = 0) is delivered right after subscribing
            awaitUntilAsserted(() -> assertThat(received)
                    .anySatisfy(state -> assertThat(state.getAvailableCreatures()).isEqualTo(0)));

            // a change to the dwelling is pushed to the active subscription as an update
            givenDwellingEvents(
                    dwellingId,
                    new AvailableCreaturesChanged(dwellingId, creatureId, 5)
            );

            // then
            awaitUntilAsserted(() -> assertThat(received)
                    .anySatisfy(state -> {
                        assertThat(state.getDwellingId()).isEqualTo(dwellingId);
                        assertThat(state.getAvailableCreatures()).isEqualTo(5);
                    }));
        } finally {
            subscription.dispose();
        }
    }

    private DwellingReadModel currentState(String dwellingId) {
        return queryGateway.query(WatchDwelling.query(GAME_ID, dwellingId), DwellingReadModel.class).join();
    }
}
