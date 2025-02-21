package com.dddheroes.heroesofddd.resourcespool.write;

import com.dddheroes.heroesofddd.resourcespool.write.deposit.DepositResources;
import com.dddheroes.heroesofddd.resourcespool.events.ResourcesDeposited;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.CannotWithdrawMoreThanDepositedResources;
import com.dddheroes.heroesofddd.resourcespool.events.ResourcesWithdrawn;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.WithdrawResources;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
public class ResourcesPool {

    @AggregateIdentifier
    private ResourcesPoolId resourcesPoolId;
    private Resources balance = Resources.empty();

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    void decide(DepositResources command) {
        apply(ResourcesDeposited.event(command.resourcesPoolId(), command.resources()));
    }

    @EventSourcingHandler
    void evolve(ResourcesDeposited event) {
        resourcesPoolId = new ResourcesPoolId(event.resourcesPoolId());
        this.balance = balance.plus(Resources.fromRaw(event.resources()));
    }

    @CommandHandler
    public void decide(WithdrawResources command) {
        new CannotWithdrawMoreThanDepositedResources(
                balance,
                command.resources()
        ).verify();
        apply(ResourcesWithdrawn.event(command.resourcesPoolId(), command.resources()));
    }

    @EventSourcingHandler
    void evolve(ResourcesWithdrawn event) {
        resourcesPoolId = new ResourcesPoolId(event.resourcesPoolId());
        this.balance = balance.minus(Resources.fromRaw(event.resources()));
    }

    ResourcesPool() {
        // required by Axon
    }
}
