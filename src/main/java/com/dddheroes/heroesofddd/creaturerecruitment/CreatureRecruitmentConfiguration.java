package com.dddheroes.heroesofddd.creaturerecruitment;

import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingSnapshotter;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.modelling.command.RepositoryProvider;
import org.springframework.beans.factory.annotation.Autowired;

@Configuration
class CreatureRecruitmentConfiguration {

    @Bean
    CommandCostResolver<RecruitCreature> recruitCreatureCostResolver() {
        return new CommandCostResolver<>() {
            @Override
            public <T extends RecruitCreature> Resources resolve(T command) {
                return command.expectedCost();
            }

            @Override
            public Class<? extends RecruitCreature> supportedCommandType() {
                return RecruitCreature.class;
            }
        };
    }

    @Bean
    DwellingSnapshotter dwellingSnapshotter(EventStore eventStore, RepositoryProvider repositoryProvider) {
        return new DwellingSnapshotter(eventStore, repositoryProvider);
    }

    @Bean
    SnapshotTriggerDefinition dwellingSnapshotTrigger(DwellingSnapshotter dwellingSnapshotter) {
        return new EventCountSnapshotTriggerDefinition(dwellingSnapshotter, 5);
    }
}
