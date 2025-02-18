package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolTest;
import com.dddheroes.heroesofddd.shared.DomainRule;
import com.dddheroes.heroesofddd.shared.ResourceType;
import org.axonframework.modelling.command.AggregateNotFoundException;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.dddheroes.heroesofddd.shared.ResourceType.*;

class WithdrawResourcesTest extends ResourcesPoolTest {

    @Test
    void givenNothingHappened_whenWithdrawResources_ThenException() {
        // given
        var givenEvents = List.of();

        // when
        var whenCommand = withdrawResources(GOLD, 1000);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(AggregateNotFoundException.class);
    }

    @Test
    void givenDepositedResources_whenWithdrawDeposited_ThenSuccess() {
        // given
        var givenEvents = List.of(
                resourcesDeposited(GOLD, 1000),
                resourcesDeposited(GEMS, 5),
                resourcesDeposited(WOOD, 10),
                resourcesDeposited(ORE, 10)
        );

        // when
        var whenCommand = withdrawResources(WOOD, 10);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(resourcesWithdrawn(WOOD, 10));
    }

    @Test
    void givenDepositedResources_whenWithdrawMoreThanDeposited_ThenException() {
        // given
        var givenEvents = List.of(
                resourcesDeposited(GOLD, 1000),
                resourcesDeposited(GEMS, 5),
                resourcesDeposited(WOOD, 10),
                resourcesDeposited(ORE, 10)
        );

        // when
        var whenCommand = withdrawResources(WOOD, 12);

        // then
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Cannot withdraw more than deposited resources");
    }

    private WithdrawResources withdrawResources(ResourceType type, Integer amount) {
        return WithdrawResources.command(resourcesPoolId.raw(), type.name(), amount);
    }

}
