package com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import com.dddheroes.heroesofddd.calendar.events.CalendarEvent;
import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.events.DayStarted;
import com.dddheroes.heroesofddd.maintenance.write.resetprocessor.StreamProcessorsOperations;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.mockito.Mockito.*;

@Import({TestcontainersConfiguration.class, CommandGatewaySpyConfiguration.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WhenWeekStartedThenProclaimWeekSymbolTest {

    private final String GAME_ID = GameId.random().raw();
    private final String PLAYER_ID = PlayerId.random().raw();

    @Autowired
    private AggregateEventPublisher aggregateEventPublisher;

    @Autowired
    private StreamProcessorsOperations streamProcessorsOperations;

    @Autowired
    private CommandGateway commandGateway;

    @MockitoBean
    private WeekSymbolCalculator weekSymbolCalculator;

    private static final int STUBBED_GROWTH = 3;

    @BeforeEach
    void resetSpy() {
        reset(commandGateway);
        when(weekSymbolCalculator.apply(any()))
                .thenReturn(WeekSymbol.of(CreatureId.of("angel"), STUBBED_GROWTH));
    }

    @Test
    void whenDayStartedForFirstDayOfTheWeek_ThenProclaimWeekSymbol() {
        // given
        var gameId = UUID.randomUUID().toString();
        var calendarId = CalendarId.of(gameId);
        givenCalendarEvents(
                calendarId,
                new DayStarted(calendarId.raw(), 1, 1, 1)
        );

        // when
        // processed by the automation

        // then
        awaitUntilAsserted(() -> verify(commandGateway, times(1))
                .send(eq(ProclaimWeekSymbol.command(calendarId.raw(), 1, 1, "angel", STUBBED_GROWTH)),
                      eq(gameMetaData()), any())
        );
    }

    @Test
    void givenDisallowedReplay_WhenReplayed_ThenShouldNotResendTheCommand() {
        // given
        var gameId = UUID.randomUUID().toString();
        var calendarId = CalendarId.of(gameId);
        givenCalendarEvents(
                calendarId,
                new DayStarted(calendarId.raw(), 1, 1, 1)
        );

        // when
        // processed by the automation

        // then
        awaitUntilAsserted(() -> verify(commandGateway, times(1))
                .send(eq(ProclaimWeekSymbol.command(calendarId.raw(), 1, 1, "angel", STUBBED_GROWTH)),
                      eq(gameMetaData()), any())
        );

        // when
        streamProcessorsOperations.reset("Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor");

        // then
        verify(commandGateway, times(1))
                .send(eq(ProclaimWeekSymbol.command(calendarId.raw(), 1, 1, "angel", STUBBED_GROWTH)),
                      eq(gameMetaData()), any());
    }

    private void givenCalendarEvents(CalendarId calendarId, CalendarEvent... events) {
        aggregateEventPublisher.publish("Calendar", calendarId.raw(), gameMetaData(), (Object[]) events);
    }

    private Metadata gameMetaData() {
        return GameMetaData.with(GAME_ID, PLAYER_ID);
    }
}
