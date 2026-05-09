package com.dddheroes.heroesofddd.resourcespool.write;

import com.dddheroes.heroesofddd.resourcespool.write.deposit.DepositResources;
import com.dddheroes.heroesofddd.resourcespool.events.ResourcesDeposited;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.CannotWithdrawMoreThanDepositedResources;
import com.dddheroes.heroesofddd.resourcespool.events.ResourcesWithdrawn;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.WithdrawResources;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;

@EventSourced(tagKey = "ResourcesPool", idType = ResourcesPoolId.class)
public class ResourcesPool {

    private ResourcesPoolId resourcesPoolId;
    private Resources balance = Resources.empty();

    @CommandHandler
    void decide(DepositResources command, EventAppender eventAppender) {
        eventAppender.append(ResourcesDeposited.event(command.resourcesPoolId(), command.resources()));
    }

    @EventSourcingHandler
    void evolve(ResourcesDeposited event) {
        resourcesPoolId = new ResourcesPoolId(event.resourcesPoolId());
        this.balance = balance.plus(Resources.fromRaw(event.resources()));
    }

    @CommandHandler
    public void decide(WithdrawResources command, EventAppender eventAppender) {
        new CannotWithdrawMoreThanDepositedResources(
                balance,
                command.resources()
        ).verify();
        eventAppender.append(ResourcesWithdrawn.event(command.resourcesPoolId(), command.resources()));
    }

    @EventSourcingHandler
    void evolve(ResourcesWithdrawn event) {
        resourcesPoolId = new ResourcesPoolId(event.resourcesPoolId());
        this.balance = balance.minus(Resources.fromRaw(event.resources()));
    }

    @EntityCreator
    ResourcesPool() {
        // required by Axon
    }

    public ResourcesPool(ResourcesPoolId resourcesPoolId) {
        this.resourcesPoolId = resourcesPoolId;
    }
}
