package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersId;
import com.dddheroes.heroesofddd.astrologers.events.WeekSymbolProclaimed;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import com.dddheroes.heroesofddd.utils.AggregateEventPublisher;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.core.Metadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.mockito.Mockito.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesTest {

    private static final String GAME_ID = GameId.random().raw();
    private static final String PLAYER_ID = PlayerId.random().raw();

    @Autowired
    private AggregateEventPublisher aggregateEventPublisher;

    @MockitoSpyBean
    private CommandGateway commandGateway;

    @Test
    void whenWeekSymbolProclaimed_thenIncreaseDwellingsAvailableCreaturesIfSymbolSameAsSymbol() {
        // given
        var angelDwellingId1 = dwellingBuiltEvent("angel");
        var angelDwellingId2 = dwellingBuiltEvent("angel");
        var titanDwellingId = dwellingBuiltEvent("titan");

        // when
        var astrologersId = AstrologersId.random();
        astrologersEvents(
                astrologersId.raw(),
                new WeekSymbolProclaimed(astrologersId.raw(), 1, 1, "angel", 3)
        );

        // then
        var expectedCommand1 = IncreaseAvailableCreatures.command(angelDwellingId1, "angel", 3);
        assertCommandExecuted(expectedCommand1);

        var expectedCommand2 = IncreaseAvailableCreatures.command(angelDwellingId2, "angel", 3);
        assertCommandExecuted(expectedCommand2);

        var notExpectedCommand = IncreaseAvailableCreatures.command(titanDwellingId, "titan", 3);
        assertCommandNotExecuted(notExpectedCommand);
    }

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
        var week1NotExpectedCommand1 = IncreaseAvailableCreatures.command(angelDwellingId2, "angel", 1);
        assertCommandNotExecuted(week1NotExpectedCommand1);

        // week 2 - 2 dwellings built
        var week2ExpectedCommand1 = IncreaseAvailableCreatures.command(angelDwellingId1, "angel", 2);
        assertCommandExecuted(week2ExpectedCommand1);
        var week2ExpectedCommand2 = IncreaseAvailableCreatures.command(angelDwellingId2, "angel", 2);
        assertCommandExecuted(week2ExpectedCommand2);
    }


    private String dwellingBuiltEvent(String creatureId) {
        var dwellingId = DwellingId.random();
        var costPerTroop = Resources.from(ResourceType.GOLD, Amount.of(1000));
        var event = DwellingBuilt.event(dwellingId, CreatureId.of(creatureId), costPerTroop);
        aggregateEventPublisher.publish("Dwelling", dwellingId.raw(), gameMetaData(), event);
        return dwellingId.raw();
    }

    private void astrologersEvents(String astrologersId, WeekSymbolProclaimed... events) {
        aggregateEventPublisher.publish("Astrologers", astrologersId, gameMetaData(), (Object[]) events);
    }

    private void assertCommandExecuted(IncreaseAvailableCreatures expectedCommand) {
        awaitUntilAsserted(() -> verify(commandGateway, times(1))
                .send(expectedCommand, gameMetaData()).resultAs(Void.class).join());
    }

    private void assertCommandNotExecuted(IncreaseAvailableCreatures notExpectedCommand) {
        verify(commandGateway, never()).send(notExpectedCommand, gameMetaData()).resultAs(Void.class).join();
    }

    private static Metadata gameMetaData() {
        return GameMetaData.with(GAME_ID, PLAYER_ID);
    }
}
