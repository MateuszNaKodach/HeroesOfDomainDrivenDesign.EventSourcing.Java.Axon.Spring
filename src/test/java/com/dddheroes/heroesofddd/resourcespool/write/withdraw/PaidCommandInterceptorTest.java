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
import org.axonframework.commandhandling.RoutingKey;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.gateway.EventGateway;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = {
        "application.interceptors.paid-commands.enabled=true"
})
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
    void givenSufficientResources_whenPaidCommandFailed_thenResourcesNotWithdrawn() {
        // given
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, "GOLD", 1000)
        );
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, "GEMS", 10)
        );

        // when
        var paidCommand = TestPaidCommand.failing(COMMAND_COST);
        assertThatThrownBy(() -> executePlayerCommand(paidCommand))
                .satisfies(e -> assertThat(e).hasMessageContaining(
                        "TestPaidCommand failed! Resources withdrawal should be rolled back"));

        // then
        eventStoreAssertions.assertEventNotStored(resourcesPoolId, ResourcesWithdrawn.class);
        eventStoreAssertions.assertNoEventsStored(paidCommand.identifier);
    }

    @Test
    void givenInsufficientResources_whenExecutingPaidCommand_thenResourcesNotWithdrewAndCommandNotExecuted() {
        // given
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, Map.of("GOLD", 200, "GEMS", 2))
        );

        // when
        var paidCommand = new TestPaidCommand(COMMAND_COST);

        // then
        assertThatThrownBy(() -> executePlayerCommand(paidCommand))
                .satisfies(e -> assertThat(e).hasMessageContaining("Cannot withdraw more than deposited resources"));
        eventStoreAssertions.assertEventNotStored(resourcesPoolId, ResourcesWithdrawn.class);
        eventStoreAssertions.assertNoEventsStored(paidCommand.identifier);
    }

    @Test
    void givenMissingResource_whenExecutingPaidCommand_thenCommandFails() {
        // given
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, Map.of("GOLD", 200))
        );

        // when
        var paidCommand = new TestPaidCommand(COMMAND_COST);

        // then
        assertThatThrownBy(() -> executePlayerCommand(paidCommand))
                .satisfies(e -> assertThat(e).hasMessageContaining("Cannot withdraw more than deposited resources"));
        eventStoreAssertions.assertEventNotStored(resourcesPoolId, ResourcesWithdrawn.class);
        eventStoreAssertions.assertNoEventsStored(paidCommand.identifier);
    }

    @Test
    void givenResources_whenExecutingNonPaidCommand_thenNoResourcesWithdrawn() {
        // given
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, "GOLD", 1000)
        );

        // when
        var nonPaidCommand = new TestNonPaidCommand();
        executePlayerCommand(nonPaidCommand);

        // then
        eventStoreAssertions.assertEventNotStored(resourcesPoolId, ResourcesWithdrawn.class);
        eventStoreAssertions.assertEventsStoredCount(nonPaidCommand.identifier, 1);
    }

    @Test
    void givenNoResources_whenExecutingPaidCommand_thenResourcesNotWithdrewAndCommandNotExecuted() {
        // when
        var paidCommand = new TestPaidCommand(COMMAND_COST);

        // then
        assertThatThrownBy(() -> executePlayerCommand(paidCommand))
                .satisfies(e -> assertThat(e).hasMessageContaining("Cannot withdraw more than deposited resources"));
        eventStoreAssertions.assertEventNotStored(resourcesPoolId, ResourcesWithdrawn.class);
        eventStoreAssertions.assertNoEventsStored(paidCommand.identifier);
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
            @RoutingKey
            String identifier,
            Map<String, Integer> cost,
            boolean failing
    ) implements Command {

        TestPaidCommand(Map<String, Integer> cost) {
            this(UUID.randomUUID().toString(), cost, false);
        }

        static TestPaidCommand failing(Map<String, Integer> cost) {
            return new TestPaidCommand(UUID.randomUUID().toString(), cost, true);
        }
    }

    record TestNonPaidCommand(@RoutingKey String identifier) implements Command {

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

            private final EventGateway eventGateway;

            TestCommandHandler(EventGateway eventGateway) {
                this.eventGateway = eventGateway;
            }

            @CommandHandler
            public void handle(TestPaidCommand command) {
                if (command.failing) {
                    throw new RuntimeException("TestPaidCommand failed! Resources withdrawal should be rolled back");
                }
                eventGateway.publish(new GenericDomainEventMessage<>("TestAggregate",
                                                                     command.identifier,
                                                                     0,
                                                                     "TestEvent"));
            }

            @CommandHandler
            public void handle(TestNonPaidCommand command) {
                eventGateway.publish(new GenericDomainEventMessage<>("TestAggregate",
                                                                     command.identifier,
                                                                     0,
                                                                     "TestEvent"));
            }
        }
    }
}