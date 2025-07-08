package com.dddheroes.heroesofddd.creaturerecruitment;

import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.eventsourcing.snapshotting.SnapshotFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    SnapshotTriggerDefinition dwellingSnapshotTrigger(Snapshotter snapshotter) {
        return new EventCountSnapshotTriggerDefinition(snapshotter, 5);
    }

    @Bean
    public SnapshotFilter dwellingSnapshotFilter() {
        return snapshotData -> {
            var type = snapshotData.getType();
            return true;
        };
    }
}
