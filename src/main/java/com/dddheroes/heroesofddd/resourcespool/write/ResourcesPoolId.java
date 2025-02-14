package com.dddheroes.heroesofddd.resourcespool.write;

import java.util.UUID;

public record ResourcesPoolId(String raw) {

    private final static String AGGREGATE_TYPE = "ResourcesPool";

    public ResourcesPoolId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Resources Pool ID cannot be null or empty");
        }
        raw = withType(raw);
    }

    public static ResourcesPoolId of(String raw) {
        return new ResourcesPoolId(raw);
    }

    public static ResourcesPoolId random() {
        return new ResourcesPoolId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return raw;
    }

    private static String withType(String id) {
        return id.startsWith(AGGREGATE_TYPE + ":") ? id : AGGREGATE_TYPE + ":" + id;
    }
}