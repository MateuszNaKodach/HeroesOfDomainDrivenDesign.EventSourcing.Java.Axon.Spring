package com.dddheroes.heroesofddd.creaturerecruitment.write;

import java.util.UUID;

public record DwellingId(String raw) {

    private final static String AGGREGATE_TYPE = "Dwelling";

    public DwellingId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Dwelling id cannot be null or empty");
        }
        raw = withType(raw);
    }

    public static DwellingId of(String raw) {
        return new DwellingId(raw);
    }

    public static DwellingId random() {
        return new DwellingId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return raw;
    }

    private static String withType(String id) {
        return id.startsWith(AGGREGATE_TYPE + ":") ? id : AGGREGATE_TYPE + ":" + id;
    }
}