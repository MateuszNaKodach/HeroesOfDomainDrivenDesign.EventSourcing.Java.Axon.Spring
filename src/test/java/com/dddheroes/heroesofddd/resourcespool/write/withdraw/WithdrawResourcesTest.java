package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolTest;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import org.axonframework.modelling.entity.AggregateNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType.*;

class WithdrawResourcesTest extends ResourcesPoolTest {

    @Test
    void givenNothingHappened_whenWithdrawResources_ThenException() {
        // given
        var givenEvents = List.of();

        // when
        var whenCommand = withdrawResources(GOLD, 1000);

        // then
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(AggregateNotFoundException.class);
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .events(resourcesWithdrawn(WOOD, 10));
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
        fixture.given()
               .events(givenEvents)
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class, "Cannot withdraw more than deposited resources");
    }

    private WithdrawResources withdrawResources(ResourceType type, Integer amount) {
        return WithdrawResources.command(resourcesPoolId.raw(), type.name(), amount);
    }

}
