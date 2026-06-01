package com.dddheroes.heroesofddd.creaturerecruitment;

import com.dddheroes.heroesofddd.creaturerecruitment.write.Dwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.eventsourcing.snapshotting.RevisionSnapshotFilter;
import org.axonframework.eventsourcing.snapshotting.SnapshotFilter;
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

    @Bean
    SnapshotTriggerDefinition dwellingSnapshotTrigger(Snapshotter snapshotter) {
        return new EventCountSnapshotTriggerDefinition(snapshotter, 5);
    }

    @Bean
    SnapshotFilter dwellingSnapshotFilter() {
        // Only load snapshots matching the current aggregate revision; stale-revision
        // snapshots are skipped and the aggregate is rebuilt from its events instead.
        // The type must match the aggregate type identifier in the event store, which
        // defaults to the aggregate's simple name (not the fully-qualified class name).
        return RevisionSnapshotFilter.builder()
                .type(Dwelling.class.getSimpleName())
                .revision(Dwelling.REVISION)
                .build();
    }

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
