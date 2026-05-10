package com.dddheroes.heroesofddd.creaturerecruitment.automation;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.armies.events.ArmyEvent;
import com.dddheroes.heroesofddd.armies.write.addcreature.AddCreatureToArmy;
import com.dddheroes.heroesofddd.armies.events.CreatureAddedToArmy;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingEvent;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.events.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited;
import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import com.dddheroes.heroesofddd.utils.AggregateEventPublisher;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.core.Metadata;
import org.junit.jupiter.api.Test;
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
    private AggregateEventPublisher aggregateEventPublisher;

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
                .send(AddCreatureToArmy.command(armyId, creatureId, 1), gameMetaData()).resultAs(Void.class).join()
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
                .send(AddCreatureToArmy.command(armyId, creatureId, 1), gameMetaData()).resultAs(Void.class).join()
        );
        awaitUntilAsserted(() -> verify(commandGateway, never())
                .send(IncreaseAvailableCreatures.command(dwellingId, creatureId, 2), gameMetaData()).resultAs(Void.class).join()
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
                .send(AddCreatureToArmy.command(armyId, creatureId, 1), gameMetaData()).resultAs(Void.class).join()
        );
        awaitUntilAsserted(() -> verify(commandGateway, times(1))
                .send(IncreaseAvailableCreatures.command(dwellingId, creatureId, 2), gameMetaData()).resultAs(Void.class).join()
        );
    }

    private void givenDwellingEvents(String dwellingId, DwellingEvent... events) {
        aggregateEventPublisher.publish("Dwelling", dwellingId, gameMetaData(), (Object[]) events);
    }

    private void givenArmyEvents(String armyId, ArmyEvent... events) {
        aggregateEventPublisher.publish("Army", armyId, gameMetaData(), (Object[]) events);
    }

    private static Metadata gameMetaData() {
        return GameMetaData.with(GAME_ID, PLAYER_ID);
    }
}
