package com.dddheroes.heroesofddd.compatibility;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;

/*
 * "Old data, new version" golden test for the Axon 5.4.0 upgrade, default JPA event store mode.
 *
 * The database is restored from src/test/resources/compat/af-5.3.0-SNAPSHOT/jpa-event-store.sql, a pg_dump written
 * by the app on Axon Framework 5.3.0-SNAPSHOT (see generate-fixture.sh next to it for the jar and the scenario).
 * The dump is restored by the postgres image itself (docker-entrypoint-initdb.d, so psql runs it, large objects
 * included) before the application context starts. The state right after the restore is read with plain JDBC and
 * kept in FIXTURE; the tests compare what the current app version does with it.
 *
 * Own container on purpose: the shared TestcontainersConfiguration database is written by other tests.
 * The nested classes run in order and share one application context: startup checks first, then new commands.
 */
@SpringBootTest
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
class JpaEventStoreDataFromAxon530CompatibilityTest {

    // Keep in sync with generate-fixture.sh.
    private static final String FIXTURE = "compat/af-5.3.0-SNAPSHOT/jpa-event-store.sql";
    private static final String GAME_ID = "compat-game";
    private static final String PLAYER_ID = "player-1";
    private static final String ARMY = "Army:army-1";
    private static final String ANGEL_DWELLING = "Dwelling:dwelling-1";
    private static final String READ_MODEL_PROCESSOR = "ReadModel_Dwelling";
    private static final String ADD_TO_ARMY_PROCESSOR = "Automation_WhenCreatureRecruitedThenAddToArmy_Processor";
    private static final String CREATURE_ADDED_TO_ARMY = "com.dddheroes.heroesofddd.armies.events.CreatureAddedToArmy";
    private static final String CREATURE_RECRUITED =
            "com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited";

    @SuppressWarnings("resource") // stopped by Testcontainers (Ryuk) when the JVM ends, like the cached context
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.13")
            .withDatabaseName("heroes")
            .withUsername("heroes")
            .withPassword("heroes")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource(FIXTURE), "/docker-entrypoint-initdb.d/jpa-event-store.sql"
            );

    private static final DatabaseState RESTORED;

    static {
        POSTGRES.start();
        RESTORED = DatabaseState.read();
    }

    @DynamicPropertySource
    static void restoredDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // the reactive read models connect through R2DBC, not the JDBC datasource
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://%s:%d/%s".formatted(
                POSTGRES.getHost(), POSTGRES.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    @Nested
    @Order(1)
    class GivenDataWrittenByAxon530 {

        @Test
        void fixtureContainsTheScenario() {
            // given
            var fixture = RESTORED;

            // when
            var armyEvents = fixture.eventsOf(ARMY);

            // then
            assertThat(fixture.events()).hasSize(8);
            assertThat(armyEvents).extracting(StoredEvent::type)
                                  .containsExactly(CREATURE_ADDED_TO_ARMY, CREATURE_ADDED_TO_ARMY);
            assertThat(fixture.tokens().keySet()).anyMatch(key -> key.startsWith(READ_MODEL_PROCESSOR + "/"))
                                                 .anyMatch(key -> key.startsWith(ADD_TO_ARMY_PROCESSOR + "/"));
            assertThat(fixture.readModel()).contains(
                    "read_model_dwelling|Dwelling:dwelling-1|2|{\"GEMS\": 1, \"GOLD\": 3000}|angel|compat-game",
                    "read_model_dwelling|Dwelling:dwelling-2|3|{\"GOLD\": 1000}|pegasus|compat-game"
            );
        }
    }

    @Nested
    @Order(2)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WhenApplicationOnTheNewVersionStarts {

        @Test
        @Order(1)
        void tokensAreTakenOverWithoutReset() {
            // given
            var restoredTokens = RESTORED.tokens();

            // when
            var tokensAfterStartup = DatabaseState.read().tokens();

            // then
            assertThat(tokensAfterStartup).isEqualTo(restoredTokens);
        }

        @Test
        @Order(2)
        void eventsAreNotReprocessed() {
            // given
            var restoredEvents = RESTORED.events();

            // when
            var settlingWindow = Duration.ofSeconds(3);

            // then
            await().during(settlingWindow)
                   .atMost(settlingWindow.plusSeconds(5))
                   .untilAsserted(() -> {
                       var current = DatabaseState.read();
                       assertThat(current.events())
                               .as("no event may be appended by processors replaying the 5.3 events")
                               .isEqualTo(restoredEvents);
                       assertThat(current.tokens()).isEqualTo(RESTORED.tokens());
                   });
        }

        @Test
        @Order(3)
        void readModelIsUnchanged() {
            // given
            var restoredReadModel = RESTORED.readModel();

            // when
            var readModel = DatabaseState.read().readModel();

            // then
            assertThat(readModel).isEqualTo(restoredReadModel);
        }
    }

    @Nested
    @Order(3)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WhenContinuingTheGameOnTheNewVersion {

        private final CommandGateway commandGateway;

        WhenContinuingTheGameOnTheNewVersion(CommandGateway commandGateway) {
            this.commandGateway = commandGateway;
        }

        @Test
        @Order(1)
        void rebuildingAnExistingDwellingIsRejectedByDomainRule() {
            // given
            var command = BuildDwelling.command(ANGEL_DWELLING, "angel", Map.of("GOLD", 3000, "GEMS", 1));

            // when
            var thrown = catchThrowable(
                    () -> commandGateway.send(command, GameMetaData.with(GAME_ID, PLAYER_ID))
                                        .resultAs(Void.class)
                                        .join()
            );

            // then
            assertThat(thrown).hasMessageContaining("Only not built building can be build");
            assertThat(DatabaseState.read().eventsOf(ANGEL_DWELLING))
                    .isEqualTo(RESTORED.eventsOf(ANGEL_DWELLING));
        }

        @Test
        @Order(2)
        void recruitingFromAnExistingDwellingAddsCreaturesToTheExistingArmy() {
            // given
            var command = RecruitCreature.command(
                    ANGEL_DWELLING, "angel", ARMY, 1, Map.of("GOLD", 3000, "GEMS", 1)
            );
            var nextDwellingSequence = RESTORED.eventsOf(ANGEL_DWELLING).size();
            var nextArmySequence = RESTORED.eventsOf(ARMY).size();

            // when
            commandGateway.send(command, GameMetaData.with(GAME_ID, PLAYER_ID)).resultAs(Void.class).join();

            // then
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                var current = DatabaseState.read();
                assertThat(current.eventsOf(ANGEL_DWELLING))
                        .last()
                        .isEqualTo(new StoredEvent(ANGEL_DWELLING, nextDwellingSequence, CREATURE_RECRUITED));
                assertThat(current.eventsOf(ARMY))
                        .as("the automation appends to the army stream written by 5.3")
                        .hasSize(nextArmySequence + 1)
                        .last()
                        .isEqualTo(new StoredEvent(ARMY, nextArmySequence, CREATURE_ADDED_TO_ARMY));
                assertThat(current.highestTokenIndexOf(READ_MODEL_PROCESSOR))
                        .isGreaterThan(RESTORED.highestTokenIndexOf(READ_MODEL_PROCESSOR));
                assertThat(current.highestTokenIndexOf(ADD_TO_ARMY_PROCESSOR))
                        .isGreaterThan(RESTORED.highestTokenIndexOf(ADD_TO_ARMY_PROCESSOR));
                assertThat(current.readModel())
                        .contains("read_model_dwelling|Dwelling:dwelling-1|1|{\"GEMS\": 1, \"GOLD\": 3000}|angel|compat-game");
            });
        }
    }

    record StoredEvent(String aggregateIdentifier, long sequenceNumber, String type) {

    }

    /**
     * Event store, token store and read model rows, read with plain JDBC so it also works before the application
     * context exists. Tokens are keyed by {@code processor/segment} and hold the token type and the serialized token,
     * owner and timestamp are left out (a running app claims and refreshes them without changing the position).
     */
    record DatabaseState(List<StoredEvent> events, Map<String, String> tokens, List<String> readModel) {

        private static final Pattern TOKEN_INDEX = Pattern.compile("\"index\":(\\d+)");

        static DatabaseState read() {
            try (var connection = DriverManager.getConnection(
                    POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                 var statement = connection.createStatement()) {
                var events = new ArrayList<StoredEvent>();
                try (var rows = statement.executeQuery(
                        "select aggregate_identifier, aggregate_sequence_number, type "
                        + "from aggregate_event_entry order by global_index")) {
                    while (rows.next()) {
                        events.add(new StoredEvent(rows.getString(1), rows.getLong(2), rows.getString(3)));
                    }
                }
                var tokens = new TreeMap<String, String>();
                try (var rows = statement.executeQuery(
                        "select processor_name, segment, token_type, convert_from(lo_get(token), 'UTF8') "
                        + "from token_entry")) {
                    while (rows.next()) {
                        tokens.put(rows.getString(1) + "/" + rows.getInt(2), rows.getString(3) + " " + rows.getString(4));
                    }
                }
                var readModel = new ArrayList<String>();
                try (var rows = statement.executeQuery(
                        "select 'read_model_dwelling', dwelling_id, available_creatures, cost_per_troop, creature_id, "
                        + "game_id from read_model_dwelling "
                        + "union all select 'read_model_built_dwelling', dwelling_id, null, null, creature_id, game_id "
                        + "from read_model_built_dwelling order by 1, 2")) {
                    while (rows.next()) {
                        var row = new ArrayList<String>();
                        for (int column = 1; column <= 6; column++) {
                            if (rows.getString(column) != null) {
                                row.add(rows.getString(column));
                            }
                        }
                        readModel.add(String.join("|", row));
                    }
                }
                return new DatabaseState(
                        List.copyOf(events), Collections.unmodifiableSortedMap(tokens), List.copyOf(readModel)
                );
            } catch (SQLException e) {
                throw new IllegalStateException("Cannot read the restored database", e);
            }
        }

        List<StoredEvent> eventsOf(String aggregateIdentifier) {
            return events.stream().filter(event -> event.aggregateIdentifier().equals(aggregateIdentifier)).toList();
        }

        long highestTokenIndexOf(String processorName) {
            return tokens.entrySet().stream()
                         .filter(token -> token.getKey().startsWith(processorName + "/"))
                         .map(token -> TOKEN_INDEX.matcher(token.getValue()))
                         .filter(Matcher::find)
                         .mapToLong(matcher -> Long.parseLong(matcher.group(1)))
                         .max()
                         .orElse(-1);
        }
    }
}
