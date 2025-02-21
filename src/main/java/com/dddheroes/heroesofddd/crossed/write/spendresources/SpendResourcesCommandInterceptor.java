package com.dddheroes.heroesofddd.crossed.write.spendresources;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.slices.write.Command;
import org.axonframework.commandhandling.RoutingKey;

import java.util.Map;

public record SpendResourcesCommandInterceptor(
        @RoutingKey ResourcesPoolId resourcesPoolId,
        Resources resources,
        Command buy
) implements Command {


}
