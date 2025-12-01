package com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModelTest;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SubscribeToAllDwellingsTest extends DwellingReadModelTest {

    private final QueryGateway queryGateway;

    @Autowired
    SubscribeToAllDwellingsTest(
            EventGateway eventGateway,
            QueryGateway queryGateway
    ) {
        super(eventGateway);
        this.queryGateway = queryGateway;
    }

    @Nested
    class WhenSubscribingToDwellings {

        @Nested
        class GivenNoDwellingsExist {

            @Test
            void shouldReceiveEmptyInitialResult() {
                // given
                var query = GetAllDwellings.query(GAME_ID);

                // when
                SubscriptionQueryResult<GetAllDwellings.Result, DwellingReadModel> subscriptionQuery =
                        queryGateway.subscriptionQuery(
                                query,
                                ResponseTypes.instanceOf(GetAllDwellings.Result.class),
                                ResponseTypes.instanceOf(DwellingReadModel.class)
                        );

                // then
                StepVerifier.create(subscriptionQuery.initialResult())
                        .assertNext(result -> assertThat(result.dwellings()).isEmpty())
                        .verifyComplete();

                subscriptionQuery.close();
            }
        }

        @Nested
        class GivenExistingDwellings {

            @Test
            void shouldReceiveInitialDwellingsAndThenUpdates() {
                // given
                var creatureId = CreatureIds.phoenix().raw();
                var existingDwellingId = DwellingId.random().raw();

                givenDwellingEvents(
                        existingDwellingId,
                        new DwellingBuilt(existingDwellingId, creatureId, PHOENIX_COST)
                );

                // Wait for initial dwelling to be projected
                await().atMost(Duration.ofSeconds(5))
                        .until(() -> {
                            var result = queryGateway.query(
                                    GetAllDwellings.query(GAME_ID),
                                    GetAllDwellings.Result.class
                            ).join();
                            return result.dwellings().size() == 1;
                        });

                var query = GetAllDwellings.query(GAME_ID);

                // when
                SubscriptionQueryResult<GetAllDwellings.Result, DwellingReadModel> subscriptionQuery =
                        queryGateway.subscriptionQuery(
                                query,
                                ResponseTypes.instanceOf(GetAllDwellings.Result.class),
                                ResponseTypes.instanceOf(DwellingReadModel.class)
                        );

                Flux<DwellingReadModel> allDwellings = subscriptionQuery.initialResult()
                        .flatMapMany(result -> Flux.fromIterable(result.dwellings()))
                        .concatWith(subscriptionQuery.updates());

                // then
                // Verify initial result and then publish a new dwelling event to test updates
                StepVerifier.create(allDwellings.take(2))
                        .assertNext(dwelling -> {
                            assertThat(dwelling.getDwellingId()).isEqualTo(existingDwellingId);
                            assertThat(dwelling.getCreatureId()).isEqualTo(creatureId);
                            assertThat(dwelling.getCostPerTroop()).isEqualTo(PHOENIX_COST);
                        })
                        .then(() -> {
                            // Publish new dwelling event
                            var newDwellingId = DwellingId.random().raw();
                            givenDwellingEvents(
                                    newDwellingId,
                                    new DwellingBuilt(newDwellingId, creatureId, PHOENIX_COST)
                            );
                        })
                        .assertNext(dwelling -> {
                            assertThat(dwelling.getCreatureId()).isEqualTo(creatureId);
                            assertThat(dwelling.getCostPerTroop()).isEqualTo(PHOENIX_COST);
                        })
                        .verifyComplete();

                subscriptionQuery.close();
            }
        }

        @Nested
        class GivenActiveSubscription {

            @Test
            void shouldReceiveUpdateWhenNewDwellingIsBuilt() {
                // given
                var query = GetAllDwellings.query(GAME_ID);
                var creatureId = CreatureIds.phoenix().raw();

                SubscriptionQueryResult<GetAllDwellings.Result, DwellingReadModel> subscriptionQuery =
                        queryGateway.subscriptionQuery(
                                query,
                                ResponseTypes.instanceOf(GetAllDwellings.Result.class),
                                ResponseTypes.instanceOf(DwellingReadModel.class)
                        );

                // when
                // Build a new dwelling after subscription is active
                var newDwellingId = DwellingId.random().raw();

                Flux<DwellingReadModel> updates = subscriptionQuery.updates().take(1);

                // Publish event after a small delay to ensure subscription is active
                Flux.defer(() -> {
                    givenDwellingEvents(
                            newDwellingId,
                            new DwellingBuilt(newDwellingId, creatureId, PHOENIX_COST)
                    );
                    return Flux.empty();
                }).delaySubscription(Duration.ofMillis(100)).subscribe();

                // then
                StepVerifier.create(updates)
                        .assertNext(dwelling -> {
                            assertThat(dwelling.getDwellingId()).isEqualTo(newDwellingId);
                            assertThat(dwelling.getCreatureId()).isEqualTo(creatureId);
                            assertThat(dwelling.getCostPerTroop()).isEqualTo(PHOENIX_COST);
                            assertThat(dwelling.getAvailableCreatures()).isEqualTo(0);
                        })
                        .verifyComplete();

                subscriptionQuery.close();
            }
        }
    }
}
