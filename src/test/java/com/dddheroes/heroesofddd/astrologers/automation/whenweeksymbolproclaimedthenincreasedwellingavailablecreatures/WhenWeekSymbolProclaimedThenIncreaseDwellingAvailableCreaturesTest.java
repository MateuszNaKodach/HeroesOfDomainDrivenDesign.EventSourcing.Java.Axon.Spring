package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.astrologers.events.AstrologersEvent;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersId;
import com.dddheroes.heroesofddd.astrologers.events.WeekSymbolProclaimed;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingEvent;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.maintenance.write.resetprocessor.StreamProcessorsOperations;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.MetaData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesTest {

    private static final String GAME_ID = GameId.random().raw();
    private static final String PLAYER_ID = PlayerId.random().raw();

    @Autowired
    private EventGateway eventGateway;

    @Autowired
    private StreamProcessorsOperations streamProcessorsOperations;

    @MockitoSpyBean
    private CommandGateway commandGateway;

    @Test
    void whenWeekSymbolProclaimed_thenIncreaseAllDwellingsBuiltBeforeTheProclamation() {
        // given
        var astrologersId = AstrologersId.random();
        var angelDwellingId1 = dwellingBuiltEvent("angel");
        astrologersEvents(
                astrologersId.raw(),
                new WeekSymbolProclaimed(astrologersId.raw(), 1, 1, "angel", 1)
        );
        var angelDwellingId2 = dwellingBuiltEvent("angel");

        // when
        astrologersEvents(
                astrologersId.raw(),
                new WeekSymbolProclaimed(astrologersId.raw(), 1, 2, "angel", 2)
        );

        // then
        // week 1 - only 1 dwelling built
        var week1ExpectedCommand1 = IncreaseAvailableCreatures.command(angelDwellingId1, "angel", 1);
        assertCommandExecuted(week1ExpectedCommand1);

        // week 2 - 2 dwellings built
        var week2ExpectedCommand1 = IncreaseAvailableCreatures.command(angelDwellingId1, "angel", 2);
        assertCommandExecuted(week2ExpectedCommand1);
        var week2ExpectedCommand2 = IncreaseAvailableCreatures.command(angelDwellingId2, "angel", 2);
        assertCommandExecuted(week2ExpectedCommand2);
    }

    @Test
    void whenWeekSymbolProclaimed_thenIncreaseOnlyMatchingCreatureDwellings() {
        // given
        var astrologersId = AstrologersId.random();
        var angelDwellingId = dwellingBuiltEvent("angel");
        var dragonDwellingId = dwellingBuiltEvent("dragon");

        // when
        astrologersEvents(
                astrologersId.raw(),
                new WeekSymbolProclaimed(astrologersId.raw(), 1, 1, "angel", 3)
        );

        // then
        var expectedCommand = IncreaseAvailableCreatures.command(angelDwellingId, "angel", 3);
        assertCommandExecuted(expectedCommand);

        // dragon dwelling should not be affected
        var notExpectedCommand = IncreaseAvailableCreatures.command(dragonDwellingId, "dragon", 3);
        assertCommandNotExecuted(notExpectedCommand);
    }

    private String dwellingBuiltEvent(String creatureId) {
        var dwellingId = DwellingId.random();
        var costPerTroop = Resources.from(ResourceType.GOLD, Amount.of(1000));
        var event = DwellingBuilt.event(dwellingId, CreatureId.of(creatureId), costPerTroop);
        eventGateway.publish(dwellingDomainEvent(dwellingId.raw(), 0, event));
        return dwellingId.raw();
    }

    private void astrologersEvents(String astrologersId, AstrologersEvent... events) {
        for (int i = 0; i < events.length; i++) {
            eventGateway.publish(astrologersDomainEvent(astrologersId, i, events[i]));
        }
    }

    private static DomainEventMessage<?> dwellingDomainEvent(String identifier, int sequenceNumber,
                                                             DwellingEvent payload) {
        return new GenericDomainEventMessage<>(
                "Dwelling",
                identifier,
                sequenceNumber,
                payload
        ).andMetaData(gameMetaData());
    }

    private static DomainEventMessage<?> astrologersDomainEvent(String identifier, int sequenceNumber,
                                                                AstrologersEvent payload) {
        return new GenericDomainEventMessage<>(
                "Astrologers",
                identifier,
                sequenceNumber,
                payload
        ).andMetaData(gameMetaData());
    }

    private static MetaData gameMetaData() {
        return GameMetaData.with(GAME_ID, PLAYER_ID);
    }

    private void assertCommandExecuted(IncreaseAvailableCreatures expectedCommand) {
        awaitUntilAsserted(() -> verify(commandGateway, times(1))
                .sendAndWait(eq(expectedCommand), eq(gameMetaData()))
        );
    }

    private void assertCommandNotExecuted(IncreaseAvailableCreatures notExpectedCommand) {
        verify(commandGateway, times(0))
                .sendAndWait(eq(notExpectedCommand), any(MetaData.class));
    }
}