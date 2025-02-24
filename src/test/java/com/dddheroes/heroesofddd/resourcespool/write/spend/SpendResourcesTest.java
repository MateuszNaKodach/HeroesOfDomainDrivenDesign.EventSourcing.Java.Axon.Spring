package com.dddheroes.heroesofddd.resourcespool.write.spend;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.resourcespool.write.deposit.DepositResources;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.messaging.MetaData;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.Map;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SpendResourcesTest {

    private static final String GAME_ID = GameId.random().raw();
    private static final String PLAYER_ID = PlayerId.random().raw();
    private static final Map<String, Integer> PHOENIX_COST = Map.of(
            "GOLD", 2000,
            "MERCURY", 1
    );

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private EventStore eventStore;

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
        var recruitCommand = RecruitCreature.command(dwellingId, creatureId, armyId, 1, Resources.from(PHOENIX_COST).raw());

        // Then
        assertDoesNotThrow(() -> commandGateway.sendAndWait(recruitCommand, gameMetaData()));
        // Verify CreatureRecruited event was stored
        var events = eventStore.readEvents(dwellingId);
        Assertions.assertTrue(events.asStream().anyMatch(event -> event.getPayload() instanceof CreatureRecruited),
                              "CreatureRecruited event should be present in event store");
    }

    @Test
    void givenInsufficientResources_whenRecruitingCreature_thenNoCreatureRecruitedEventStored() {
        // Given
        var resourcesPoolId = ResourcesPoolId.random().raw();
        var dwellingId = DwellingId.random().raw();
        var armyId = ArmyId.random().raw();
        var creatureId = CreatureIds.phoenix().raw();

        // Initialize resources pool with insufficient resources
        commandGateway.sendAndWait(
                DepositResources.command(resourcesPoolId, "GOLD", 1000), // Only half of required gold
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
        var recruitCommand = RecruitCreature.command(dwellingId, creatureId, armyId, 1, Resources.from(PHOENIX_COST).raw());

        // Then
        assertThrows(Exception.class, () ->
                commandGateway.sendAndWait(recruitCommand, gameMetaData())
        );

        // Verify no CreatureRecruited event was stored
        var events = eventStore.readEvents(dwellingId);
        Assertions.assertFalse(events.asStream().anyMatch(event -> event.getPayload() instanceof CreatureRecruited),
                               "CreatureRecruited event should not be present in event store");
    }

    private static MetaData gameMetaData() {
        return MetaData.with("gameId", GAME_ID)
                       .and("playerId", PLAYER_ID);
    }

}