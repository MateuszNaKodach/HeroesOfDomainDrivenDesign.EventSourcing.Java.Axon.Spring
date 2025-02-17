package com.dddheroes.heroesofddd.creaturerecruitment.automation;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.armies.write.ArmyEvent;
import com.dddheroes.heroesofddd.armies.write.addcreature.AddCreatureToArmy;
import com.dddheroes.heroesofddd.armies.write.addcreature.CreatureAddedToArmy;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingEvent;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.CreatureRecruited;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.shared.GameId;
import com.dddheroes.heroesofddd.shared.GameMetaData;
import com.dddheroes.heroesofddd.shared.PlayerId;
import com.dddheroes.heroesofddd.shared.ResourceType;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.MetaData;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Map;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.mockito.Mockito.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class WhenCreatureRecruitedThenAddToArmyTest {

    private static final String GAME_ID = GameId.random().raw();
    private static final String PLAYER_ID = PlayerId.random().raw();
    private static final Map<String, Integer> PHOENIX_COST = Map.of(
            ResourceType.GOLD.name(), 2000,
            ResourceType.MERCURY.name(), 1
    );

    @Autowired
    private EventGateway eventGateway;

    @MockitoSpyBean
    private CommandGateway commandGateway;

    @Test
    void whenCreatureRecruited_ThenAddCreatureToArmy() {
        // given
        var dwellingId = DwellingId.random().raw();
        var creatureId = CreatureIds.phoenix().raw();
        var armyId = ArmyId.random().raw();
        givenDwellingEvents(
                dwellingId,
                new DwellingBuilt(dwellingId, creatureId, PHOENIX_COST),
                new AvailableCreaturesChanged(dwellingId, creatureId, 3),
                new CreatureRecruited(dwellingId, creatureId, armyId, 1, PHOENIX_COST)
        );

        // when
        // processed by the automation

        // then
        awaitUntilAsserted(() -> verify(commandGateway, times(1))
                .sendAndWait(AddCreatureToArmy.command(armyId, creatureId, 1), gameMetaData())
        );
    }

    @Test
    void givenArmyIsNotFull_whenCreatureRecruited_ThenAddToTheArmyAndDoNotCompensate() {
        // given
        var armyId = ArmyId.random().raw();
        givenArmyEvents(
                armyId,
                new CreatureAddedToArmy(armyId, CreatureIds.angel().raw(), 1),
                new CreatureAddedToArmy(armyId, CreatureIds.behemoth().raw(), 1),
                new CreatureAddedToArmy(armyId, CreatureIds.bowman().raw(), 1),
                new CreatureAddedToArmy(armyId, CreatureIds.redDragon().raw(), 1),
                new CreatureAddedToArmy(armyId, CreatureIds.blackDragon().raw(), 1),
                new CreatureAddedToArmy(armyId, CreatureIds.archAngel().raw(), 1)
        );
        // when
        var dwellingId = DwellingId.random().raw();
        var creatureId = CreatureIds.phoenix().raw();
        givenDwellingEvents(
                dwellingId,
                new DwellingBuilt(dwellingId, creatureId, PHOENIX_COST),
                new AvailableCreaturesChanged(dwellingId, creatureId, 3),
                new CreatureRecruited(dwellingId, creatureId, armyId, 1, PHOENIX_COST)
        );

        // and
        // processed by the automation

        // then
        awaitUntilAsserted(() -> verify(commandGateway, times(1))
                .sendAndWait(AddCreatureToArmy.command(armyId, creatureId, 1), gameMetaData())
        );
        awaitUntilAsserted(() -> verify(commandGateway, never())
                .sendAndWait(IncreaseAvailableCreatures.command(dwellingId, creatureId, 2), gameMetaData())
        );
    }

    @Test
    void givenArmyIsFull_whenCreatureRecruited_ThenDoNotAddToTheArmyAndCompensateRecruitment() {
        // given
        var armyId = ArmyId.random().raw();
        givenArmyEvents(
                armyId,
                new CreatureAddedToArmy(armyId, CreatureIds.angel().raw(), 1),
                new CreatureAddedToArmy(armyId, CreatureIds.behemoth().raw(), 1),
                new CreatureAddedToArmy(armyId, CreatureIds.bowman().raw(), 1),
                new CreatureAddedToArmy(armyId, CreatureIds.redDragon().raw(), 1),
                new CreatureAddedToArmy(armyId, CreatureIds.blackDragon().raw(), 1),
                new CreatureAddedToArmy(armyId, CreatureIds.archAngel().raw(), 1),
                new CreatureAddedToArmy(armyId, CreatureIds.centaur().raw(), 1)
        );
        // when
        var dwellingId = DwellingId.random().raw();
        var creatureId = CreatureIds.phoenix().raw();
        givenDwellingEvents(
                dwellingId,
                new DwellingBuilt(dwellingId, creatureId, PHOENIX_COST),
                new AvailableCreaturesChanged(dwellingId, creatureId, 3),
                new CreatureRecruited(dwellingId, creatureId, armyId, 2, PHOENIX_COST)
        );

        // and
        // processed by the automation

        // then
        awaitUntilAsserted(() -> verify(commandGateway, never())
                .sendAndWait(AddCreatureToArmy.command(armyId, creatureId, 1), gameMetaData())
        );
        awaitUntilAsserted(() -> verify(commandGateway, times(1))
                .sendAndWait(IncreaseAvailableCreatures.command(dwellingId, creatureId, 2), gameMetaData())
        );
    }

    private void givenDwellingEvents(String dwellingId, DwellingEvent... events) {
        for (int i = 0; i < events.length; i++) {
            eventGateway.publish(dwellingDomainEvent(dwellingId, i, events[i]));
        }
    }

    private DomainEventMessage<?> dwellingDomainEvent(String dwellingId, int sequenceNumber, DwellingEvent payload) {
        return new GenericDomainEventMessage<>(
                "Dwelling",
                dwellingId,
                sequenceNumber,
                payload
        ).andMetaData(gameMetaData());
    }

    private void givenArmyEvents(String armyId, ArmyEvent... events) {
        for (int i = 0; i < events.length; i++) {
            eventGateway.publish(armyDomainEvent(armyId, i, events[i]));
        }
    }


    private DomainEventMessage<?> armyDomainEvent(String armyId, int sequenceNumber, ArmyEvent payload) {
        return new GenericDomainEventMessage<>(
                "Army",
                armyId,
                sequenceNumber,
                payload
        ).andMetaData(gameMetaData());
    }

    private static MetaData gameMetaData() {
        return GameMetaData.with(GAME_ID, PLAYER_ID);
    }
}