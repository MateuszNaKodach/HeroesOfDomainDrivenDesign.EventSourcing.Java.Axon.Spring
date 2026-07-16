package com.dddheroes.heroesofddd.shared.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.r2dbc.postgresql.codec.Json;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/**
 * Maps the {@code jsonb} read-model columns (e.g. {@code read_model_dwelling.cost_per_troop}) to
 * {@code Map<String, Integer>}. The Postgres R2DBC driver exposes {@code jsonb} values as
 * {@link Json}, which Spring Data R2DBC cannot convert out of the box.
 */
@Configuration
class R2dbcJsonbConfiguration {

    @Bean
    R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        var dialect = DialectResolver.getDialect(connectionFactory);
        return R2dbcCustomConversions.of(dialect, List.of(
                new MapToJsonConverter(objectMapper),
                new JsonToMapConverter(objectMapper)
        ));
    }

    @WritingConverter
    static class MapToJsonConverter implements Converter<Map<String, Integer>, Json> {

        private final ObjectMapper objectMapper;

        MapToJsonConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Json convert(Map<String, Integer> source) {
            try {
                return Json.of(objectMapper.writeValueAsString(source));
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot serialize map to jsonb", e);
            }
        }
    }

    @ReadingConverter
    static class JsonToMapConverter implements Converter<Json, Map<String, Integer>> {

        private final ObjectMapper objectMapper;

        JsonToMapConverter(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Map<String, Integer> convert(Json source) {
            try {
                return objectMapper.readValue(source.asString(), new TypeReference<>() {
                });
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot deserialize jsonb to map", e);
            }
        }
    }
}
