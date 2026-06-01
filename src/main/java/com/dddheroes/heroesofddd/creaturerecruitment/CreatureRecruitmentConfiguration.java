package com.dddheroes.heroesofddd.creaturerecruitment;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.resourcespool.application.CommandCostResolver;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.fasterxml.jackson.core.JsonGenerator;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.core.MessageType;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.conversion.MessageConverter;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;
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
    CommandCostResolver recruitCreatureCostResolver() {
        return new CommandCostResolver() {
            @Override
            public Resources resolve(CommandMessage message, ProcessingContext context) {
                var converter = context.component(MessageConverter.class);
                return message.payloadAs(RecruitCreature.class, converter).expectedCost();
            }

            @Override
            public QualifiedName supportedCommand() {
                return new MessageType(RecruitCreature.class).qualifiedName();
            }
        };
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
