package com.dddheroes.heroesofddd.resourcespool.write;

import com.dddheroes.heroesofddd.resourcespool.events.ResourcesDeposited;
import com.dddheroes.heroesofddd.resourcespool.events.ResourcesWithdrawn;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule;
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer;
import org.axonframework.test.fixture.AxonTestFixture;
import org.junit.jupiter.api.*;

public class ResourcesPoolTest {

    protected final ResourcesPoolId resourcesPoolId = ResourcesPoolId.random();
    protected AxonTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = AxonTestFixture.with(
                EventSourcingConfigurer.create()
                                       .registerEntity(EventSourcedEntityModule.autodetected(ResourcesPoolId.class, ResourcesPool.class))
        );
    }

    @AfterEach
    void tearDown() {
        fixture.stop();
    }

    protected ResourcesDeposited resourcesDeposited(ResourceType type, Integer amount) {
        return ResourcesDeposited.event(resourcesPoolId, Resources.from(type, Amount.of(amount)));
    }

    protected ResourcesWithdrawn resourcesWithdrawn(ResourceType type, Integer amount) {
        return ResourcesWithdrawn.event(resourcesPoolId, Resources.from(type, Amount.of(amount)));
    }
}
