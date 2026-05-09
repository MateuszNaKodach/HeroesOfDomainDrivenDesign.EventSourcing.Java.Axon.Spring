package com.dddheroes.heroesofddd.astrologers.write;

import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule;
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer;
import org.axonframework.test.fixture.AxonTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class AstrologersTest {

    protected AxonTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = AxonTestFixture.with(EventSourcingConfigurer.create().registerEntity(EventSourcedEntityModule.autodetected(AstrologersId.class, Astrologers.class)));
    }

    @AfterEach
    void tearDown() {
        fixture.stop();
    }
}