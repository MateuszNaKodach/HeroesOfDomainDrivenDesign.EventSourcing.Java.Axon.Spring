package com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import com.dddheroes.heroesofddd.calendar.write.CalendarEvent;
import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.events.DayStarted;
import com.dddheroes.heroesofddd.maintenance.write.resetprocessor.StreamProcessorsOperations;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
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

import java.util.UUID;

import static com.dddheroes.heroesofddd.utils.AwaitilityUtils.awaitUntilAsserted;
import static org.mockito.Mockito.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class WhenWeekStartedThenProclaimWeekSymbolTest {

    private static final String GAME_ID = GameId.random().raw();
    private static final String PLAYER_ID = PlayerId.random().raw();

    @Autowired
    private EventGateway eventGateway;

    @Autowired
    private StreamProcessorsOperations streamProcessorsOperations;

    @MockitoSpyBean
    private CommandGateway commandGateway;

    @Test
    void whenDayStartedForFirstDayOfTheWeek_ThenProclaimWeekSymbol() {
        // given
        var gameId = UUID.randomUUID().toString();
        var calendarId = CalendarId.of(gameId);
        givenCalendarEvents(
                gameId,
                new DayStarted(calendarId.raw(), 1, 1, 1)
        );

        // when
        // processed by the automation

        // then
        awaitUntilAsserted(() -> verify(commandGateway, times(1))
                .sendAndWait(ProclaimWeekSymbol.command(gameId, 1, 1, "angel", any()), eq(gameMetaData()))
        );
    }

    @Test
    void givenDisallowedReplay_WhenReplayed_ThenShouldNotResendTheCommand() {
        // given
        var gameId = UUID.randomUUID().toString();
        var calendarId = CalendarId.of(gameId);
        givenCalendarEvents(
                gameId,
                new DayStarted(calendarId.raw(), 1, 1, 1)
        );

        // when
        // processed by the automation

        // then
        awaitUntilAsserted(() -> verify(commandGateway, times(1))
                .sendAndWait(ProclaimWeekSymbol.command(gameId, 1, 1, "angel", any()), eq(gameMetaData()))
        );

        // when
        streamProcessorsOperations.reset("Automation_WhenWeekStartedThenProclaimWeekSymbol_Processor");

        // then
        verify(commandGateway, times(1))
                .sendAndWait(ProclaimWeekSymbol.command(gameId, 1, 1, "angel", any()), eq(gameMetaData()));
    }

    private void givenCalendarEvents(String gameId, CalendarEvent... events) {
        for (int i = 0; i < events.length; i++) {
            eventGateway.publish(calendarDomainEvent(gameId, i, events[i]));
        }
    }

    private static DomainEventMessage<?> calendarDomainEvent(String identifier, int sequenceNumber,
                                                             CalendarEvent payload) {
        return new GenericDomainEventMessage<>(
                "Calendar",
                identifier,
                sequenceNumber,
                payload
        ).andMetaData(gameMetaData());
    }

    private static MetaData gameMetaData() {
        return GameMetaData.with(GAME_ID, PLAYER_ID);
    }
}