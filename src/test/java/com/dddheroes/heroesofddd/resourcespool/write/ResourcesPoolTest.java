package com.dddheroes.heroesofddd.resourcespool.write;

import com.dddheroes.heroesofddd.resourcespool.write.deposit.ResourcesDeposited;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.ResourcesWithdrawn;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ResourceType;
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
        return ResourcesDeposited.event(resourcesPoolId, type, Amount.of(amount));
    }

    protected ResourcesWithdrawn resourcesWithdrawn(ResourceType type, Integer amount) {
        return ResourcesWithdrawn.event(resourcesPoolId, type, Amount.of(amount));
    }
}
