package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.resourcespool.events.ResourcesWithdrawn;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.resourcespool.write.deposit.DepositResources;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.slices.write.Command;
import com.dddheroes.heroesofddd.utils.EventStoreAssertions;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.MetaData;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RecruitCreatureRequiresResourcesTest {

    private static final String GAME_ID = GameId.random().raw();
    private static final String PLAYER_ID = PlayerId.random().raw();
    private static final Map<String, Integer> PHOENIX_COST = Map.of(
            "GOLD", 2000,
            "MERCURY", 1
    );

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private EventStoreAssertions eventStoreAssertions;

    @Test
    void whenRecruitingCreature_thenProcessPaymentAndRecruitSuccessfully() {
        // given
        var resourcesPoolId = playerResourcesPoolId();
        var dwellingId = DwellingId.random().raw();
        var armyId = ArmyId.random().raw();
        var creatureId = CreatureIds.phoenix().raw();

        // initialize resources pool with sufficient resources
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, "GOLD", 3000)
        );
        executePlayerCommand(
                DepositResources.command(resourcesPoolId, "MERCURY", 2)
        );

        // build dwelling and make creatures available
        executePlayerCommand(
                BuildDwelling.command(dwellingId, creatureId, PHOENIX_COST)
        );
        executePlayerCommand(
                IncreaseAvailableCreatures.command(dwellingId, creatureId, 5)
        );

        // when
        var recruitCommand = RecruitCreature.command(
                dwellingId,
                creatureId,
                armyId,
                1,
                Resources.from(PHOENIX_COST).raw()
        );

        // then
        assertDoesNotThrow(() -> executePlayerCommand(recruitCommand));
        eventStoreAssertions.assertEventStored(dwellingId, CreatureRecruited.class);
        eventStoreAssertions.assertEventNotStored(dwellingId, ResourcesWithdrawn.class);
    }

    @Test
    void givenInsufficientResources_whenRecruitingCreature_thenNoCreatureRecruitedEventStored() {
        // given
        var resourcesPoolId = playerResourcesPoolId();
        var dwellingId = DwellingId.random().raw();
        var armyId = ArmyId.random().raw();
        var creatureId = CreatureIds.phoenix().raw();

        // initialize resources pool with insufficient resources
        executePlayerCommand(DepositResources.command(resourcesPoolId, "GOLD", 1000));
        executePlayerCommand(DepositResources.command(resourcesPoolId, "MERCURY", 2));

        // build dwelling and make creatures available
        executePlayerCommand(
                BuildDwelling.command(dwellingId, creatureId, PHOENIX_COST)
        );
        executePlayerCommand(
                IncreaseAvailableCreatures.command(dwellingId, creatureId, 5)
        );

        // when
        var recruitCommand = RecruitCreature.command(
                dwellingId,
                creatureId,
                armyId,
                1,
                Resources.from(PHOENIX_COST).raw()
        );

        // then
        assertThatThrownBy(() -> executePlayerCommand(recruitCommand))
                .cause()
                .satisfies(e -> assertThat(e).hasMessageContaining("Cannot withdraw more than deposited resources"));
        eventStoreAssertions.assertEventNotStored(dwellingId, CreatureRecruited.class);
        eventStoreAssertions.assertEventNotStored(dwellingId, ResourcesWithdrawn.class);
    }

    private void executePlayerCommand(Command command) {
        commandGateway.sendAndWait(command, gameMetaData());
    }

    private static MetaData gameMetaData() {
        return MetaData.with("gameId", GAME_ID)
                       .and("playerId", PLAYER_ID);
    }

    private static String playerResourcesPoolId() {
        return ResourcesPoolId.of(PLAYER_ID).raw();
    }
}