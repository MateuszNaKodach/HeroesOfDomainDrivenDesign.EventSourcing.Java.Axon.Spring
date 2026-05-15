package com.dddheroes.heroesofddd.creaturerecruitment;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

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

    // NOTE: The dwellingSnapshotTrigger bean (EventCountSnapshotTriggerDefinition, N=5) is now dead code
    // after the AF4→AF5 migration. Snapshotting in AF5 requires explicit EventSourcedEntityModule
    // registration via declarative(...).snapshotPolicy(c -> SnapshotPolicy.afterEvents(5)).build(),
    // which is not supported through Spring's @EventSourced auto-detection path.
    // Snapshotting can be re-enabled by providing a declarative EventSourcedEntityModule @Bean
    // (without @EventSourced on the entity class) once the Spring extension supports it.

    @Bean
    public Module dwellingIdSerializationModule() {
        return new DwellingIdSerializationModule();
    }

    private static class DwellingIdSerializationModule extends SimpleModule {

        public DwellingIdSerializationModule() {
            addSerializer(DwellingId.class, new JsonSerializer<>() {
                @Override
                public void serialize(DwellingId value, JsonGenerator gen, SerializerProvider __) throws IOException {
                    gen.writeString(value.raw());
                }
            });
            addDeserializer(DwellingId.class, new JsonDeserializer<>() {
                @Override
                public DwellingId deserialize(JsonParser p, DeserializationContext __) throws IOException {
                    return new DwellingId(p.getValueAsString());
                }
            });
        }

    }
}
