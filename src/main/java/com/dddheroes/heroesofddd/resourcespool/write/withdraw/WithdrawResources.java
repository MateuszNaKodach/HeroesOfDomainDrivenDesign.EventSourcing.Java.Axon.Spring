package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolCommand;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ResourceType;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record WithdrawResources(
        @TargetAggregateIdentifier
        ResourcesPoolId resourcesPoolId,
        ResourceType type,
        Amount amount
) implements ResourcesPoolCommand {

    public static WithdrawResources command(String resourcesPoolId, String type, Integer amount) {
        return new WithdrawResources(ResourcesPoolId.of(resourcesPoolId), ResourceType.from(type), Amount.of(amount));
    }
}
