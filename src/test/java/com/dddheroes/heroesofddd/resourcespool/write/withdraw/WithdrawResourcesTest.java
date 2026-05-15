package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolTest;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType.*;

class WithdrawResourcesTest extends ResourcesPoolTest {

    @Test
    void givenNothingHappened_whenWithdrawResources_ThenException() {
        // given
        // when
        var whenCommand = withdrawResources(GOLD, 1000);

        // then
        fixture.given()
               .noPriorActivity()
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class);
    }

    @Test
    void givenDepositedResources_whenWithdrawDeposited_ThenSuccess() {
        // given
        // when
        var whenCommand = withdrawResources(WOOD, 10);

        // then
        fixture.given()
               .events(
                       resourcesDeposited(GOLD, 1000),
                       resourcesDeposited(GEMS, 5),
                       resourcesDeposited(WOOD, 10),
                       resourcesDeposited(ORE, 10)
               )
               .when()
               .command(whenCommand)
               .then()
               .events(resourcesWithdrawn(WOOD, 10));
    }

    @Test
    void givenDepositedResources_whenWithdrawMoreThanDeposited_ThenException() {
        // given
        // when
        var whenCommand = withdrawResources(WOOD, 12);

        // then
        fixture.given()
               .events(
                       resourcesDeposited(GOLD, 1000),
                       resourcesDeposited(GEMS, 5),
                       resourcesDeposited(WOOD, 10),
                       resourcesDeposited(ORE, 10)
               )
               .when()
               .command(whenCommand)
               .then()
               .exception(DomainRule.ViolatedException.class);
    }

    private WithdrawResources withdrawResources(ResourceType type, Integer amount) {
        return WithdrawResources.command(resourcesPoolId.raw(), type.name(), amount);
    }

}
