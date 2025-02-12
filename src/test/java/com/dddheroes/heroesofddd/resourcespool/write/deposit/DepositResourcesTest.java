package com.dddheroes.heroesofddd.resourcespool.write.deposit;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolTest;
import com.dddheroes.heroesofddd.shared.ResourceType;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.dddheroes.heroesofddd.shared.ResourceType.*;
import static com.dddheroes.heroesofddd.shared.ResourceType.ORE;

class DepositResourcesTest extends ResourcesPoolTest {

    @Test
    void givenNothingHappened_whenDepositResources_ThenSuccess() {
        // given
        var givenEvents = List.of();

        // when
        var whenCommand = depositResources(GOLD, 1000);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(resourcesDeposited(GOLD, 1000));
    }

    @Test
    void givenDepositedResources_whenDepositMoreResources_ThenSuccess() {
        // given
        var givenEvents = List.of(
                resourcesDeposited(GOLD, 1000),
                resourcesDeposited(GEMS, 5),
                resourcesDeposited(WOOD, 10),
                resourcesDeposited(ORE, 10)
        );

        // when
        var whenCommand = depositResources(WOOD, 2);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(resourcesDeposited(WOOD, 2));
    }

    private DepositResources depositResources(ResourceType type, Integer amount) {
        return DepositResources.command(resourcesPoolId.raw(), type.name(), amount);
    }

}
