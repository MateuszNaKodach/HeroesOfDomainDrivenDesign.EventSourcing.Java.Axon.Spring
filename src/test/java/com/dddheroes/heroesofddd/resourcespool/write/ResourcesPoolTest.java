package com.dddheroes.heroesofddd.resourcespool.write;

import com.dddheroes.heroesofddd.resourcespool.events.ResourcesDeposited;
import com.dddheroes.heroesofddd.resourcespool.events.ResourcesWithdrawn;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.*;

public class ResourcesPoolTest {

    protected final ResourcesPoolId resourcesPoolId = ResourcesPoolId.random();
    protected AggregateTestFixture<?> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(ResourcesPool.class);
    }

    protected ResourcesDeposited resourcesDeposited(ResourceType type, Integer amount) {
        return ResourcesDeposited.event(resourcesPoolId, Resources.from(type, Amount.of(amount)));
    }

    protected ResourcesWithdrawn resourcesWithdrawn(ResourceType type, Integer amount) {
        return ResourcesWithdrawn.event(resourcesPoolId, Resources.from(type, Amount.of(amount)));
    }
}
