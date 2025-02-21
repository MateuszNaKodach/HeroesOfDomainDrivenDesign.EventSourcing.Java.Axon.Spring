package com.dddheroes.heroesofddd.crossed.write.spendresources;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.slices.write.Command;
import org.axonframework.commandhandling.RoutingKey;

import java.util.Map;

// todo: it may be on top of others, or used and extended/ implemented. Like PaidCommand.
record SpendResources(
        @RoutingKey ResourcesPoolId resourcesPoolId,
        Resources resources,
        Command buy
) implements Command {

    public static SpendResourcesCommandInterceptor command(
            String resourcesPoolId,
            Map<String, Integer> resources,
            Command buy
    ) {
        return new SpendResourcesCommandInterceptor(
                ResourcesPoolId.of(resourcesPoolId),
                Resources.from(resources),
                buy
        );
    }
}
