package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.resourcespool.events.ResourcesWithdrawn;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.resourcespool.write.deposit.DepositResources;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.slices.write.Command;
import com.dddheroes.heroesofddd.utils.EventStoreAssertions;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.MetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// todo: test resources not withdrew if test command failed

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PaidCommandInterceptorTest {

    private static final String GAME_ID = GameId.random().raw();
    private static final Map<String, Integer> COMMAND_COST = Map.of(
            "GOLD", 500,
            "GEMS", 5
    );
    private String playerId;
    private String resourcesPoolId;

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private EventStoreAssertions eventStoreAssertions;

    @Autowired
    private EventStore eventStore;


    @BeforeEach
    void setUp() {
        playerId = PlayerId.random().raw();
        resourcesPoolId = playerResourcesPoolId();
    }

    @Test
    void givenSufficientResources_whenExecutingPaidCommand_thenResourcesWithdrawn() {
        // given
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, "GOLD", 1000)
        );
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, "GEMS", 10)
        );

        // when
        var paidCommand = new TestPaidCommand(COMMAND_COST);
        executePlayerCommand(paidCommand);

        // then
        eventStoreAssertions.assertEventStored(
                resourcesPoolId,
                ResourcesWithdrawn.event(
                        ResourcesPoolId.of(resourcesPoolId),
                        Resources.from(COMMAND_COST)
                )
        );
    }

    @Test
    void givenInsufficientResources_whenExecutingPaidCommand_thenResourcesNotWithdrewAndCommandNotExecuted() {
        // given
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, Map.of("GOLD", 200, "GEMS", 2))
        );

        // record the number of events before attempting the paid command
        long eventCountBefore = eventStore.readEvents(resourcesPoolId).asStream().count();

        // when/then
        var paidCommand = new TestPaidCommand(COMMAND_COST);

        assertThatThrownBy(() -> executePlayerCommand(paidCommand))
                .cause()
                .satisfies(e -> assertThat(e).hasMessageContaining("Cannot withdraw more than deposited resources"));

        // Verify no new ResourcesWithdrawn events were stored
        long eventCountAfter = eventStore.readEvents(resourcesPoolId).asStream().count();

        // The count of events should be the same as before, indicating no ResourcesWithdrawn event was added
        assertThat(eventCountAfter).isEqualTo(eventCountBefore);
    }

    @Test
    void givenMissingResource_whenExecutingPaidCommand_thenCommandFails() {
        // given
        // initialize resources pool with just gold but missing gems entirely
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, "GOLD", 1000)
        );
        // no gems deposited at all

        // record the event count before the command
        long eventCountBefore = eventStore.readEvents(resourcesPoolId).asStream().count();

        // when/then
        var paidCommand = new TestPaidCommand(COMMAND_COST);

        assertThatThrownBy(() -> executePlayerCommand(paidCommand))
                .cause()
                .satisfies(e -> assertThat(e).hasMessageContaining("Cannot withdraw more than deposited resources"));

        // Verify no new ResourcesWithdrawn events were stored
        long eventCountAfter = eventStore.readEvents(resourcesPoolId).asStream().count();

        // The count of events should be the same as before
        assertThat(eventCountAfter).isEqualTo(eventCountBefore);
    }

    @Test
    void givenResources_whenExecutingNonPaidCommand_thenNoResourcesWithdrawn() {
        // given
        // initialize resources pool with resources
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, "GOLD", 1000)
        );

        // Count ResourcesWithdrawn events before executing the command
        long withdrawnEventsBefore = eventStore.readEvents(resourcesPoolId)
                                               .asStream()
                                               .filter(event -> event.getPayload() instanceof ResourcesWithdrawn)
                                               .count();

        // when
        var nonPaidCommand = new TestNonPaidCommand();
        executePlayerCommand(nonPaidCommand);

        // then
        // Count ResourcesWithdrawn events after executing the command
        long withdrawnEventsAfter = eventStore.readEvents(resourcesPoolId)
                                              .asStream()
                                              .filter(event -> event.getPayload() instanceof ResourcesWithdrawn)
                                              .count();

        // Verify no new ResourcesWithdrawn events were added
        assertThat(withdrawnEventsAfter).isEqualTo(withdrawnEventsBefore);
    }

    private void executePlayerCommand(Command command) {
        commandGateway.sendAndWait(command, gameMetaData());
    }

    private MetaData gameMetaData() {
        return MetaData.with("gameId", GAME_ID)
                       .and("playerId", playerId);
    }

    private String playerResourcesPoolId() {
        return ResourcesPoolId.of(playerId).raw();
    }

    record TestPaidCommand(
            Map<String, Integer> cost,
            boolean failing
    ) implements Command {

        TestPaidCommand(Map<String, Integer> cost) {
            this(cost, false);
        }

        static TestPaidCommand failing(Map<String, Integer> cost) {
            return new TestPaidCommand(cost, true);
        }
    }

    record TestNonPaidCommand(String identifier) implements Command {

        TestNonPaidCommand() {
            this(UUID.randomUUID().toString());
        }
    }


    @TestConfiguration
    static class TestConfig {

        @Component
        @Primary
        static class TestCommandCostResolver
                implements CommandCostResolver<PaidCommandInterceptorTest.TestPaidCommand> {

            @Override
            public <T extends PaidCommandInterceptorTest.TestPaidCommand> Resources resolve(T command) {
                if (command instanceof PaidCommandInterceptorTest.TestPaidCommand paidCommand) {
                    return Resources.from(paidCommand.cost());
                }
                return Resources.empty();
            }

            @Override
            public Class<? extends PaidCommandInterceptorTest.TestPaidCommand> supportedCommandType() {
                return PaidCommandInterceptorTest.TestPaidCommand.class;
            }
        }

        @Component
        static class TestCommandHandler {

            @CommandHandler
            public void handle(TestPaidCommand command) {
                if (command.failing) {
                    throw new RuntimeException("TestPaidCommand failed! Resources withdrawal should be rolled back");
                }
            }

            @CommandHandler
            public void handle(TestNonPaidCommand command) {
                // handle the command
            }
        }
    }
}