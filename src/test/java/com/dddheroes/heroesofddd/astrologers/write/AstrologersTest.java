package com.dddheroes.heroesofddd.astrologers.write;

import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.BeforeEach;

public class AstrologersTest {

    protected AggregateTestFixture<?> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(Astrologers.class);
    }
}