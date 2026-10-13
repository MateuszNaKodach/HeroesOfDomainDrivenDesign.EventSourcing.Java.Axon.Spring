package com.dddheroes.heroesofddd.shared.infrastructure;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.utils.EventStoreAssertions;
import org.axonframework.conversion.ConversionException;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.concurrent.CompletionException;

import static com.dddheroes.heroesofddd.utils.AxonUtils.aGameMetaData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/*
 * Canary for regression 1 of the Axon 5.4.0 upgrade (docs/UPGRADE-AXON-5.4.0.md): a void command sent over the
 * Axon Server command bus completes with a result that resultAs(Void.class) cannot convert.
 *
 * Each nested class has its own @SpringBootTest properties, so each gets its own application context (and its own
 * Axon Server and Postgres containers), and the workaround toggle is honoured per context.
 *
 * - WithWorkaroundEnabled: the app's real behavior. Must always pass.
 * - WithWorkaroundDisabled: documents the upstream bug. When a future Axon / Axoniq Framework release fixes
 *   CommandConverter, this test fails. That is the signal to delete
 *   AxonServerVoidCommandResultWorkaroundConfiguration, its unit test and this nested class.
 */
class AxonServerVoidCommandResultCanaryTest {

    private static final String WORKAROUND_PROPERTY = "application.workarounds.axon-server-void-command-result.enabled";

    @Nested
    @Import(TestcontainersConfiguration.class)
    @ActiveProfiles("axonserver-aggregate")
    @SpringBootTest
    class WithWorkaroundEnabled {

        private final CommandGateway commandGateway;
        private final EventStoreAssertions eventStoreAssertions;

        WithWorkaroundEnabled(CommandGateway commandGateway, EventStoreAssertions eventStoreAssertions) {
            this.commandGateway = commandGateway;
            this.eventStoreAssertions = eventStoreAssertions;
        }

        @Test
        void voidCommandResultCompletesWithNullAndEventIsStored() {
            // given
            var dwellingId = DwellingId.random().raw();
            var command = buildAngelDwelling(dwellingId);

            // when
            var result = commandGateway.send(command, aGameMetaData()).resultAs(Void.class).join();

            // then
            assertThat(result).isNull();
            eventStoreAssertions.assertEventStored("Dwelling", dwellingId, DwellingBuilt.class);
        }
    }

    @Nested
    @Import(TestcontainersConfiguration.class)
    @ActiveProfiles("axonserver-aggregate")
    @SpringBootTest(properties = WORKAROUND_PROPERTY + "=false")
    class WithWorkaroundDisabled {

        private static final String DELETE_THE_WORKAROUND =
                "The Axon Server connector no longer turns a void command result into an empty byte[]. "
                + "The upstream regression is fixed: delete AxonServerVoidCommandResultWorkaroundConfiguration, "
                + "EmptyCommandResultAsNullInterceptorTest and this nested class, and update "
                + "docs/UPGRADE-AXON-5.4.0.md.";

        private final CommandGateway commandGateway;
        private final EventStoreAssertions eventStoreAssertions;

        WithWorkaroundDisabled(CommandGateway commandGateway, EventStoreAssertions eventStoreAssertions) {
            this.commandGateway = commandGateway;
            this.eventStoreAssertions = eventStoreAssertions;
        }

        @Test
        void upstreamVoidResultRegressionStillPresent_deleteWorkaroundWhenThisFails() {
            // given
            var dwellingId = DwellingId.random().raw();
            var command = buildAngelDwelling(dwellingId);

            // when
            var thrown = catchThrowable(
                    () -> commandGateway.send(command, aGameMetaData()).resultAs(Void.class).join()
            );

            // then
            assertThat(thrown)
                    .as(DELETE_THE_WORKAROUND)
                    .isInstanceOf(CompletionException.class)
                    .cause()
                    .isInstanceOf(ConversionException.class)
                    .hasMessageContaining("'byte[]' to 'java.lang.Void'");
            // the command itself was handled, only reading its result fails
            eventStoreAssertions.assertEventStored("Dwelling", dwellingId, DwellingBuilt.class);
        }
    }

    private static BuildDwelling buildAngelDwelling(String dwellingId) {
        return BuildDwelling.command(dwellingId, CreatureIds.angel().raw(), Map.of("GOLD", 3000, "GEMS", 1));
    }
}
