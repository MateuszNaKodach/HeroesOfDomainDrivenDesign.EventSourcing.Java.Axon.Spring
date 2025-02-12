package com.dddheroes.heroesofddd.resourcespool.write;

import com.dddheroes.heroesofddd.resourcespool.write.deposit.DepositResources;
import com.dddheroes.heroesofddd.resourcespool.write.deposit.ResourcesDeposited;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ResourceType;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.Map;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
public class ResourcesPool {

    @AggregateIdentifier
    private ResourcesPoolId resourcesPoolId;
    private Map<ResourceType, Amount> resources;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    void decide(DepositResources command) {
        apply(ResourcesDeposited.event(command.resourcesPoolId(), command.type(), command.amount()));
    }

    @EventSourcingHandler
    void evolve(ResourcesDeposited event) {
        resourcesPoolId = ResourcesPoolId.of(event.resourcesPoolId());
        resources.merge(ResourceType.from(event.type()), new Amount(event.amount()), Amount::plus);
    }

    ResourcesPool() {
        // required by Axon
    }
}
