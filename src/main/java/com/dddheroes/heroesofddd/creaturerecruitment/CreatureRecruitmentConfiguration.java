package com.dddheroes.heroesofddd.creaturerecruitment;

import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
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
}
