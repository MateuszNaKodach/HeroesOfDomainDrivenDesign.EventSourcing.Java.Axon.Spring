package com.dddheroes.heroesofddd.shared.domain.identifiers;

import java.util.UUID;

public record PlayerId(String raw) {

    public PlayerId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Player id cannot be null or empty");
        }
    }

    public static PlayerId of(String raw) {
        return new PlayerId(raw);
    }

    public static PlayerId random() {
        return new PlayerId(UUID.randomUUID().toString());
    }

    public static PlayerId unknown() {
        return new PlayerId("unknown");
    }

    @Override
    public String toString() {
        return raw;
    }
}