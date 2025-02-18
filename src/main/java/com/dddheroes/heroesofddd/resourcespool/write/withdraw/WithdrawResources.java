package com.dddheroes.heroesofddd.resourcespool.write.withdraw;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolCommand;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.Resources;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

import java.util.Map;

public record WithdrawResources(
        @TargetAggregateIdentifier
        ResourcesPoolId resourcesPoolId,
        Resources resources
) implements ResourcesPoolCommand {

    public static WithdrawResources command(String resourcesPoolId, String type, Integer amount) {
        return command(resourcesPoolId, Map.of(type, amount));
    }

    public static WithdrawResources command(String resourcesPoolId, Map<String, Integer> resources) {
        return new WithdrawResources(ResourcesPoolId.of(resourcesPoolId), Resources.fromRaw(resources));
    }
}
