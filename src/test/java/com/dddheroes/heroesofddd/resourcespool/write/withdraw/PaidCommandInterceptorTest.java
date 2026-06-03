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
import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.core.Metadata;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.conversion.MessageConverter;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
import org.axonframework.messaging.eventhandling.annotation.Event;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
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
                "ResourcesPool",
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
        eventStoreAssertions.assertEventNotStored("ResourcesPool", resourcesPoolId, ResourcesWithdrawn.class);
        eventStoreAssertions.assertNoEventsStored("TestAggregate", paidCommand.identifier);
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
        eventStoreAssertions.assertEventNotStored("ResourcesPool", resourcesPoolId, ResourcesWithdrawn.class);
        eventStoreAssertions.assertNoEventsStored("TestAggregate", paidCommand.identifier);
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
        eventStoreAssertions.assertEventNotStored("ResourcesPool", resourcesPoolId, ResourcesWithdrawn.class);
        eventStoreAssertions.assertNoEventsStored("TestAggregate", paidCommand.identifier);
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
        eventStoreAssertions.assertEventNotStored("ResourcesPool", resourcesPoolId, ResourcesWithdrawn.class);
        eventStoreAssertions.assertEventsStoredCount("TestAggregate", nonPaidCommand.identifier, 1);
    }

    @Test
    void givenNoResources_whenExecutingPaidCommand_thenResourcesNotWithdrewAndCommandNotExecuted() {
        // when
        var paidCommand = new TestPaidCommand(COMMAND_COST);

        // then
        assertThatThrownBy(() -> executePlayerCommand(paidCommand))
                .satisfies(e -> assertThat(e).hasMessageContaining("Cannot withdraw more than deposited resources"));
        eventStoreAssertions.assertEventNotStored("ResourcesPool", resourcesPoolId, ResourcesWithdrawn.class);
        eventStoreAssertions.assertNoEventsStored("TestAggregate", paidCommand.identifier);
    }

    private void executePlayerCommand(Command command) {
        commandGateway.send(command, gameMetaData()).resultAs(Void.class).join();
    }

    private Metadata gameMetaData() {
        return Metadata.with("gameId", GAME_ID)
                       .and("playerId", playerId);
    }

    private String playerResourcesPoolId() {
        return ResourcesPoolId.of(playerId).raw();
    }

    @org.axonframework.messaging.commandhandling.annotation.Command(routingKey = "identifier")
    record TestPaidCommand(
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

    @org.axonframework.messaging.commandhandling.annotation.Command(routingKey = "identifier")
    record TestNonPaidCommand(String identifier) implements Command {

        TestNonPaidCommand() {
            this(UUID.randomUUID().toString());
        }
    }

    @Event
    record TestEvent(
            @EventTag(key = "TestAggregate")
            String identifier
    ) {}


    @TestConfiguration
    static class TestConfig {

        @Component
        @Primary
        static class TestCommandCostResolver implements CommandCostResolver {

            @Override
            public Resources resolve(CommandMessage message, ProcessingContext context) {
                var converter = context.component(MessageConverter.class);
                var command = message.payloadAs(PaidCommandInterceptorTest.TestPaidCommand.class, converter);
                return Resources.from(command.cost());
            }

            @Override
            public QualifiedName supportedCommand() {
                // TestPaidCommand is a nested record: the @Command annotation resolves its message name from
                // getPackageName() + getSimpleName() (without the enclosing class), while new MessageType(Class)
                // uses Class.getName() (with the enclosing class and '$') - so the names would never match.
                // Build the QualifiedName the same way the annotation-based resolver does.
                return new QualifiedName(TestPaidCommand.class.getPackageName(),
                                         TestPaidCommand.class.getSimpleName());
            }
        }

        @Component
        static class TestCommandHandler {

            @CommandHandler
            public void handle(TestPaidCommand command, EventAppender eventAppender) {
                if (command.failing) {
                    throw new RuntimeException("TestPaidCommand failed! Resources withdrawal should be rolled back");
                }
                eventAppender.append(new TestEvent(command.identifier));
            }

            @CommandHandler
            public void handle(TestNonPaidCommand command, EventAppender eventAppender) {
                eventAppender.append(new TestEvent(command.identifier));
            }
        }
    }
}