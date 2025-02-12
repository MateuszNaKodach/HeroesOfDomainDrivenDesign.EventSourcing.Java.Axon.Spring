package com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import com.dddheroes.heroesofddd.calendar.write.CalendarEvent;
import com.dddheroes.heroesofddd.calendar.write.CalendarId;
import com.dddheroes.heroesofddd.calendar.write.startday.DayStarted;
import com.dddheroes.heroesofddd.shared.GameId;
import com.dddheroes.heroesofddd.shared.GameMetaData;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.messaging.MessageType;
import org.axonframework.messaging.unitofwork.ProcessingContext;
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

    @Autowired
    private EventGateway eventGateway;

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
                .send(ProclaimWeekSymbol.command(gameId, 1, 1, "angel", any()), eq(GameMetaData.withId(GAME_ID)), any(ProcessingContext.class))
        );
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
                new MessageType("test", "event", "0.0.1"),
                payload
        ).andMetaData(GameMetaData.withId(GAME_ID));
    }
}