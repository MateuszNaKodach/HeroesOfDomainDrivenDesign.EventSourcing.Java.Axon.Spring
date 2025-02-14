package com.dddheroes.heroesofddd.shared;

import java.util.UUID;

public record ArmyId(String raw) {

    private final static String AGGREGATE_TYPE = "Army";

    public ArmyId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Army id cannot be null or empty");
        }
        raw = withType(raw);
    }

    public static ArmyId of(String raw) {
        return new ArmyId(raw);
    }

    public static ArmyId random() {
        return new ArmyId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return raw;
    }

    private static String withType(String id) {
        return id.startsWith(AGGREGATE_TYPE + ":") ? id : AGGREGATE_TYPE + ":" + id;
    }
}