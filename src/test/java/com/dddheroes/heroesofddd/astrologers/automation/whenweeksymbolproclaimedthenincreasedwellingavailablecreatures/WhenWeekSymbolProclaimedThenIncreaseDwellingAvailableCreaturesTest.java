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
import com.dddheroes.heroesofddd.utils.CommandGatewaySpyConfiguration;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.core.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@Import({TestcontainersConfiguration.class, CommandGatewaySpyConfiguration.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesTest {

    private final String GAME_ID = GameId.random().raw();
    private final String PLAYER_ID = PlayerId.random().raw();

    @Autowired
    private AggregateEventPublisher aggregateEventPublisher;

    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private BuiltDwellingReadModelRepository builtDwellingReadModelRepository;

    @BeforeEach
    void resetSpy() {
        reset(commandGateway);
    }

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
        // given - week 1: only dwelling 1 exists
        var astrologersId = AstrologersId.random();
        var angelDwellingId1 = dwellingBuiltEvent("angel");
        astrologersEvents(
                astrologersId.raw(),
                new WeekSymbolProclaimed(astrologersId.raw(), 1, 1, "angel", 1)
        );

        // then - week 1 dispatch must complete before we widen the world
        var week1ExpectedCommand1 = IncreaseAvailableCreatures.command(angelDwellingId1, "angel", 1);
        assertCommandExecuted(week1ExpectedCommand1);
        var week1NotExpectedCommand1 = IncreaseAvailableCreatures.command(
                DwellingId.random().raw(), "angel", 1);
        assertCommandNotExecuted(week1NotExpectedCommand1);

        // and - dwelling 2 is added between proclamations
        var angelDwellingId2 = dwellingBuiltEvent("angel");
        awaitDwellingProjected(angelDwellingId2);

        // when - week 2 proclamation should now reach BOTH dwellings
        astrologersEvents(
                astrologersId.raw(),
                new WeekSymbolProclaimed(astrologersId.raw(), 1, 2, "angel", 2)
        );

        // then - week 2 - 2 dwellings built
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

    private void awaitDwellingProjected(String dwellingId) {
        awaitUntilAsserted(() ->
                assertThat(builtDwellingReadModelRepository.findById(dwellingId).blockOptional()).isPresent()
        );
    }

    private void assertCommandExecuted(IncreaseAvailableCreatures expectedCommand) {
        awaitUntilAsserted(() -> verify(commandGateway, times(1))
                .send(eq(expectedCommand), eq(gameMetaData()), any()));
    }

    private void assertCommandNotExecuted(IncreaseAvailableCreatures notExpectedCommand) {
        verify(commandGateway, never()).send(eq(notExpectedCommand), eq(gameMetaData()), any());
    }

    private Metadata gameMetaData() {
        return GameMetaData.with(GAME_ID, PLAYER_ID);
    }
}
