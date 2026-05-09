package com.dddheroes.heroesofddd.resourcespool.write.deposit;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolCommand;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.messaging.commandhandling.annotation.Command;
import org.axonframework.modelling.annotation.TargetEntityId;

import java.util.Map;

@Command
public record DepositResources(
        @TargetEntityId
        ResourcesPoolId resourcesPoolId,
        Resources resources
) implements ResourcesPoolCommand {

    public static DepositResources command(String resourcesPoolId, String type, Integer amount) {
        return command(resourcesPoolId, Map.of(type, amount));
    }

    public static DepositResources command(String resourcesPoolId, Map<String, Integer> resources) {
        return new DepositResources(ResourcesPoolId.of(resourcesPoolId), Resources.fromRaw(resources));
    }
}
