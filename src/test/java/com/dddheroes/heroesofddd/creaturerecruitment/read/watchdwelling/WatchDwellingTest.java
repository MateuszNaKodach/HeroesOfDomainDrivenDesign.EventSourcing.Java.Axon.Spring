package com.dddheroes.heroesofddd.creaturerecruitment.read.watchdwelling;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelTest;
import com.dddheroes.heroesofddd.creaturerecruitment.events.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.concurrent.CopyOnWriteArrayList;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Demonstrates the subscription query type: {@code queryGateway.subscriptionQuery(...)} returning a
 * {@code SubscriptionQueryResult} that combines the initial read-model state with live updates.
 * Updates are emitted by {@code DwellingReadModelProjector} whenever the dwelling changes.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class WatchDwellingTest extends DwellingReadModelTest {

    private final QueryGateway queryGateway;

    @Autowired
    WatchDwellingTest(
            EventGateway eventGateway,
            QueryGateway queryGateway
    ) {
        super(eventGateway);
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
        SubscriptionQueryResult<DwellingReadModel, DwellingReadModel> subscription = queryGateway.subscriptionQuery(
                WatchDwelling.query(GAME_ID, dwellingId),
                ResponseTypes.instanceOf(DwellingReadModel.class),
                ResponseTypes.instanceOf(DwellingReadModel.class)
        );
        var received = new CopyOnWriteArrayList<DwellingReadModel>();
        Disposable disposable = Flux.concat(subscription.initialResult(), subscription.updates())
                                    .subscribe(received::add);

        try {
            // the initial result (availableCreatures = 0) is delivered right after subscribing
            awaitUntilAsserted(() -> assertThat(received)
                    .anySatisfy(state -> assertThat(state.getAvailableCreatures()).isEqualTo(0)));

            // a change to the dwelling is pushed to the active subscription as an update
            eventGateway.publish(dwellingDomainEvent(
                    dwellingId, 1, new AvailableCreaturesChanged(dwellingId, creatureId, 5)
            ));

            // then
            awaitUntilAsserted(() -> assertThat(received)
                    .anySatisfy(state -> {
                        assertThat(state.getDwellingId()).isEqualTo(dwellingId);
                        assertThat(state.getAvailableCreatures()).isEqualTo(5);
                    }));
        } finally {
            disposable.dispose();
            subscription.close();
        }
    }

    private DwellingReadModel currentState(String dwellingId) {
        return queryGateway.query(WatchDwelling.query(GAME_ID, dwellingId), DwellingReadModel.class).join();
    }
}
