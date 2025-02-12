package com.dddheroes.heroesofddd.resourcespool.write;

import java.util.UUID;

public record ResourcesPoolId(String raw) {

    public ResourcesPoolId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Resources Pool ID cannot be null or empty");
        }
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
}