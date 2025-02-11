package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersEvent;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersId;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.WeekSymbolProclaimed;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingEvent;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.Cost;
import com.dddheroes.heroesofddd.shared.CreatureId;
import com.dddheroes.heroesofddd.shared.GameId;
import com.dddheroes.heroesofddd.shared.GameMetaData;
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

import java.util.UUID;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesTest {

    private static final String GAME_ID = GameId.random().raw();

    @Autowired
    private EventGateway eventGateway;

    @MockitoSpyBean
    private CommandGateway commandGateway;

    @Test
    void test() {
        // given
        var angelDwellingId1 = givenDwellingBuiltEvent("angel");
        var angelDwellingId2 = givenDwellingBuiltEvent("angel");
        var titanDwellingId = givenDwellingBuiltEvent("titan");

        // when
        var astrologersId = AstrologersId.random();
        whenAstrologersEvents(
                astrologersId.raw(),
                new WeekSymbolProclaimed(astrologersId.raw(), 1, 1, "angel", 3)
        );

        // then
        var expectedCommand1 = IncreaseAvailableCreatures.command(angelDwellingId1, "angel", 3);
        awaitUntilAsserted(() -> verify(commandGateway, times(1)).sendAndWait(expectedCommand1, GameMetaData.withId(GAME_ID)));

        var expectedCommand2 = IncreaseAvailableCreatures.command(angelDwellingId2, "angel", 3);
        awaitUntilAsserted(() -> verify(commandGateway, times(1)).sendAndWait(expectedCommand2, GameMetaData.withId(GAME_ID)));

        var notExpectedCommand = IncreaseAvailableCreatures.command(titanDwellingId, "titan", 3);
        awaitUntilAsserted(() -> verify(commandGateway, never()).sendAndWait(notExpectedCommand, GameMetaData.withId(GAME_ID)));
    }

    private String givenDwellingBuiltEvent(String creatureId) {
        var dwellingId = DwellingId.random();
        var costPerTroop = Cost.resources(ResourceType.GOLD, Amount.of(1000));
        var event = DwellingBuilt.event(dwellingId, CreatureId.of(creatureId), costPerTroop);
        eventGateway.publish(dwellingDomainEvent(dwellingId.raw(), 0, event));
        return dwellingId.raw();
    }

    private void whenAstrologersEvents(String gameId, AstrologersEvent... events) {
        for (int i = 0; i < events.length; i++) {
            eventGateway.publish(astrologersDomainEvent(gameId, i, events[i]));
        }
    }

    private static DomainEventMessage<?> astrologersDomainEvent(String identifier, int sequenceNumber,
                                                                AstrologersEvent payload) {
        return givenDomainEvent(
                "Astrologers",
                identifier,
                sequenceNumber,
                payload
        );
    }

    private static DomainEventMessage<?> dwellingDomainEvent(
            String identifier,
            int sequenceNumber,
            DwellingEvent payload
    ) {
        return givenDomainEvent(
                "Dwelling",
                identifier,
                sequenceNumber,
                payload
        );
    }

    private static DomainEventMessage<?> givenDomainEvent(
            String aggregateType,
            String identifier,
            int sequenceNumber,
            Object payload
    ) {
        return new GenericDomainEventMessage<>(
                aggregateType,
                identifier,
                sequenceNumber,
                payload
        ).andMetaData(GameMetaData.withId(GAME_ID));
    }
}