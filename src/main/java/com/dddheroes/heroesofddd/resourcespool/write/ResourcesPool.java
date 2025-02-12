package com.dddheroes.heroesofddd.resourcespool.write;

import com.dddheroes.heroesofddd.resourcespool.write.deposit.DepositResources;
import com.dddheroes.heroesofddd.resourcespool.write.deposit.ResourcesDeposited;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.CannotWithdrawMoreThanDepositedResources;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.ResourcesWithdrawn;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.WithdrawResources;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ResourceType;
import com.dddheroes.heroesofddd.shared.Resources;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
class ResourcesPool {

    @AggregateIdentifier
    private ResourcesPoolId resourcesPoolId;
    private Resources balance = Resources.empty();

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    void decide(DepositResources command) {
        apply(ResourcesDeposited.event(command.resourcesPoolId(), command.type(), command.amount()));
    }

    @EventSourcingHandler
    void evolve(ResourcesDeposited event) {
        resourcesPoolId = new ResourcesPoolId(event.resourcesPoolId());
        this.balance = balance.plus(ResourceType.from(event.type()), new Amount(event.amount()));
    }

    @CommandHandler
    void decide(WithdrawResources command) {
        new CannotWithdrawMoreThanDepositedResources(
                balance,
                Resources.from(command.type(), command.amount())
        ).verify();
        apply(ResourcesWithdrawn.event(command.resourcesPoolId(), command.type(), command.amount()));
    }

    @EventSourcingHandler
    void evolve(ResourcesWithdrawn event) {
        resourcesPoolId = new ResourcesPoolId(event.resourcesPoolId());
        this.balance = balance.minus(ResourceType.from(event.type()), new Amount(event.amount()));
    }

    ResourcesPool() {
        // required by Axon
    }
}
