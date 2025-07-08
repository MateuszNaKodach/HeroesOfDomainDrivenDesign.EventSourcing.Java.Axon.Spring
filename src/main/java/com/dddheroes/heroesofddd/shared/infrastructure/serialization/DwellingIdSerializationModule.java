package com.dddheroes.heroesofddd.shared.infrastructure.serialization;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;

public class DwellingIdSerializationModule extends SimpleModule {

    public DwellingIdSerializationModule() {
        addSerializer(DwellingId.class, new DwellingIdSerializer());
        addDeserializer(DwellingId.class, new DwellingIdDeserializer());
    }

    private static class DwellingIdSerializer extends JsonSerializer<DwellingId> {
        @Override
        public void serialize(DwellingId value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.raw());
        }
    }

    private static class DwellingIdDeserializer extends JsonDeserializer<DwellingId> {
        @Override
        public DwellingId deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = p.getValueAsString();
            return DwellingId.of(value);
        }
    }
}
