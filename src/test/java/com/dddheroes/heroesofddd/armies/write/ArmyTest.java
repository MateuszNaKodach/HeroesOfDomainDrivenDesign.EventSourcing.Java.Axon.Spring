package com.dddheroes.heroesofddd.armies.write;

import com.dddheroes.heroesofddd.shared.ArmyId;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.*;

public class ArmyTest {

    protected final ArmyId armyId = ArmyId.random();

    protected AggregateTestFixture<?> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(Army.class);
    }

}
