package com.dddheroes.heroesofddd.shared.payments;

import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.WithdrawResources;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.config.Configuration;
import org.axonframework.modelling.command.Repository;
import org.springframework.stereotype.Component;

@Component
class PaidCommandHandler {

    private final Repository<ResourcesPool> resourcesPoolRepository;
    private final Configuration configuration;

    PaidCommandHandler(Configuration configuration) {
        this.configuration = configuration;
        this.resourcesPoolRepository = configuration.repository(ResourcesPool.class);
    }

    @CommandHandler
    void handle(PaidCommand command) throws Exception {
        var payload = command.toBuy();
        var aggregateId = payload.aggregateId();
        var aggregateType = payload.aggregateType();

        var resourcesPoolId = command.resourcesPoolId().raw();
        var cost = command.cost();
        var gold = cost.raw().getOrDefault("GOLD", 0);

        resourcesPoolRepository.load(resourcesPoolId)
                               .execute(resourcesPool -> resourcesPool.decide(
                                       WithdrawResources.command(resourcesPoolId, "GOLD", gold))
                               );

        var repository = configuration.repository(aggregateType);
        repository.load(aggregateId)
                  .handle(new GenericCommandMessage<>(payload));
    }
}
