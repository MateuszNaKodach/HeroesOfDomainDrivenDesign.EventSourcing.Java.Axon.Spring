package com.dddheroes.heroesofddd.shared.infrastructure;

import com.dddheroes.heroesofddd.shared.domain.identifiers.ArmyId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class SerializationConfiguration {

    @Bean
    public Module gameIdSerializationModule() {
        return new GameIdSerializationModule();
    }

    @Bean
    public Module playerIdSerializationModule() {
        return new PlayerIdSerializationModule();
    }

    @Bean
    public Module creatureIdSerializationModule() {
        return new CreatureIdSerializationModule();
    }

    @Bean
    public Module armyIdSerializationModule() {
        return new ArmyIdSerializationModule();
    }

    @Bean
    public Module amountSerializationModule() {
        return new AmountSerializationModule();
    }

    private static class GameIdSerializationModule extends SimpleModule {

        public GameIdSerializationModule() {
            addSerializer(GameId.class, new JsonSerializer<>() {
                @Override
                public void serialize(GameId value, JsonGenerator gen, SerializerProvider __) throws IOException {
                    gen.writeString(value.raw());
                }
            });
            addDeserializer(GameId.class, new JsonDeserializer<>() {
                @Override
                public GameId deserialize(JsonParser p, DeserializationContext __) throws IOException {
                    return new GameId(p.getValueAsString());
                }
            });
        }

    }

    private static class PlayerIdSerializationModule extends SimpleModule {

        public PlayerIdSerializationModule() {
            addSerializer(PlayerId.class, new JsonSerializer<>() {
                @Override
                public void serialize(PlayerId value, JsonGenerator gen, SerializerProvider __) throws IOException {
                    gen.writeString(value.raw());
                }
            });
            addDeserializer(PlayerId.class, new JsonDeserializer<>() {
                @Override
                public PlayerId deserialize(JsonParser p, DeserializationContext __) throws IOException {
                    return new PlayerId(p.getValueAsString());
                }
            });
        }
    }

    private static class CreatureIdSerializationModule extends SimpleModule {

        public CreatureIdSerializationModule() {
            addSerializer(CreatureId.class, new JsonSerializer<>() {
                @Override
                public void serialize(CreatureId value, JsonGenerator gen, SerializerProvider __) throws IOException {
                    gen.writeString(value.raw());
                }
            });
            addDeserializer(CreatureId.class, new JsonDeserializer<>() {
                @Override
                public CreatureId deserialize(JsonParser p, DeserializationContext __) throws IOException {
                    return new CreatureId(p.getValueAsString());
                }
            });
        }

    }

    private static class ArmyIdSerializationModule extends SimpleModule {

        public ArmyIdSerializationModule() {
            addSerializer(ArmyId.class, new JsonSerializer<>() {
                @Override
                public void serialize(ArmyId value, JsonGenerator gen, SerializerProvider __) throws IOException {
                    gen.writeString(value.raw());
                }
            });
            addDeserializer(ArmyId.class, new JsonDeserializer<>() {
                @Override
                public ArmyId deserialize(JsonParser p, DeserializationContext __) throws IOException {
                    return new ArmyId(p.getValueAsString());
                }
            });
        }

    }

    private static class AmountSerializationModule extends SimpleModule {

        public AmountSerializationModule() {
            addSerializer(Amount.class, new JsonSerializer<>() {
                @Override
                public void serialize(Amount value, JsonGenerator gen, SerializerProvider __) throws IOException {
                    gen.writeNumber(value.raw());
                }
            });
            addDeserializer(Amount.class, new JsonDeserializer<>() {
                @Override
                public Amount deserialize(JsonParser p, DeserializationContext __) throws IOException {
                    return new Amount(p.getValueAsInt());
                }
            });
        }

    }
}
