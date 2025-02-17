package com.dddheroes.heroesofddd.shared.payments;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPoolId;
import com.dddheroes.heroesofddd.shared.Resources;
import org.axonframework.commandhandling.RoutingKey;

record PaidCommand(
        @RoutingKey
        ResourcesPoolId resourcesPoolId,
        Resources cost,
        Payload toBuy) {

    record Payload(Class<?> aggregateType,
                   String aggregateId,
                   Object command) {

    }
}
