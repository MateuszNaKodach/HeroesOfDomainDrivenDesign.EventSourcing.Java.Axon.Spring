package com.dddheroes.heroesofddd.shared.payments;

import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.resourcespool.write.ResourcesPool;
import com.dddheroes.heroesofddd.resourcespool.write.withdraw.WithdrawResources;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.config.Configuration;
import org.axonframework.modelling.command.Aggregate;
import org.axonframework.modelling.command.Repository;
import org.springframework.stereotype.Component;

// mozna to zrobic tak, ze command implementuja interfajs PaidCommand with Cost i wtedy mam interceptor, ktory zabiera kase
// i robic to transakcyjnie.
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

        var repository = configuration.repository(aggregateType);
        var aggregate = repository.load(aggregateId);
        aggregate.handle(new GenericCommandMessage<RecruitCreature>((RecruitCreature) payload.command()));

        resourcesPoolRepository.load(resourcesPoolId)
                               .execute(resourcesPool -> resourcesPool.decide(
                                       WithdrawResources.command(resourcesPoolId, "GOLD", gold))
                               );
    }
}
