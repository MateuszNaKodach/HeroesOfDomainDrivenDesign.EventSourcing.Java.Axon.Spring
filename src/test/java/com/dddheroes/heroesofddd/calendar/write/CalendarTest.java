package com.dddheroes.heroesofddd.calendar.write;

import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule;
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer;
import org.axonframework.messaging.commandhandling.configuration.CommandHandlingModule;
import org.axonframework.test.fixture.AxonTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class CalendarTest {

    protected AxonTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = AxonTestFixture.with(EventSourcingConfigurer.create()
                .registerEntity(EventSourcedEntityModule.autodetected(CalendarId.class, Calendar.class))
                .registerCommandHandlingModule(() -> CommandHandlingModule.named("calendar-test")
                        .commandHandlers(handlers -> handlers.autodetectedCommandHandlingComponent(
                                configuration -> new CalendarCommandHandler()))
                        .build()));
    }

    @AfterEach
    void tearDown() {
        fixture.stop();
    }
}
