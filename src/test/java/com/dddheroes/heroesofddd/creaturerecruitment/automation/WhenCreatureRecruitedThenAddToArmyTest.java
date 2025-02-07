package com.dddheroes.heroesofddd.creaturerecruitment.automation;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.armies.write.addcreature.AddCreatureToArmy;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingEvent;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.CreatureRecruited;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureIds;
import com.dddheroes.heroesofddd.shared.ResourceType;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.gateway.EventGateway;
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
                .sendAndWait(AddCreatureToArmy.command(armyId, creatureId, 1))
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
        );
    }
}