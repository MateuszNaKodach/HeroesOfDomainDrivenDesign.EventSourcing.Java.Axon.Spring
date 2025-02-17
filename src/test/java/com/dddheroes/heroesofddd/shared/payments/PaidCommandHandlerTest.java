package com.dddheroes.heroesofddd.shared.payments;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.write.Dwelling;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.resourcespool.write.deposit.DepositResources;
import com.dddheroes.heroesofddd.shared.Resources;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.shared.GameId;
import com.dddheroes.heroesofddd.shared.PlayerId;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.MetaData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PaidCommandHandlerTest {

    private static final String GAME_ID = GameId.random().raw();
    private static final String PLAYER_ID = PlayerId.random().raw();
    private static final Map<String, Integer> PHOENIX_COST = Map.of(
            "GOLD", 2000,
            "MERCURY", 1
    );

    @Autowired
    private CommandGateway commandGateway;

    @Test
    void whenRecruitingCreature_thenProcessPaymentAndRecruitSuccessfully() {
        // Given
        var resourcesPoolId = ResourcesPoolId.random().raw();
        var dwellingId = DwellingId.random().raw();
        var armyId = ArmyId.random().raw();
        var creatureId = CreatureIds.phoenix().raw();

        // Initialize resources pool with required resources
        commandGateway.sendAndWait(
                DepositResources.command(resourcesPoolId, "GOLD", 3000),
                gameMetaData()
        );
        commandGateway.sendAndWait(
                DepositResources.command(resourcesPoolId, "MERCURY", 2),
                gameMetaData()
        );

        // Build dwelling and make creatures available
        commandGateway.sendAndWait(
                BuildDwelling.command(dwellingId, creatureId, PHOENIX_COST),
                gameMetaData()
        );
        commandGateway.sendAndWait(
                IncreaseAvailableCreatures.command(dwellingId, creatureId, 5),
                gameMetaData()
        );

        // When
        // Create recruitment command
        var recruitCommand = RecruitCreature.command(dwellingId, creatureId, armyId, 1);

        // Create paid command wrapper
        var paidCommand = new PaidCommand(
                new ResourcesPoolId(resourcesPoolId),
                Resources.fromRaw(PHOENIX_COST),
                new PaidCommand.Payload(
                        Dwelling.class,
                        dwellingId,
                        recruitCommand
                )
        );

        // Then
        assertDoesNotThrow(() ->
                                   commandGateway.sendAndWait(paidCommand, gameMetaData())
        );
    }

    private static MetaData gameMetaData() {
        return MetaData.with("gameId", GAME_ID)
                       .and("playerId", PLAYER_ID);
    }
}