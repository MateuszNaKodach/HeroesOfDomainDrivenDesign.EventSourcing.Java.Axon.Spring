package com.dddheroes.heroesofddd.calendar.write;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.*;

public class CalendarTest {

    protected AggregateTestFixture<?> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(Calendar.class);
    }
}