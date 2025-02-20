package com.dddheroes.heroesofddd.shared.domain.identifiers;

import java.util.UUID;

public record GameId(String raw) {

    public GameId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Game id cannot be null or empty");
        }
    }

    public static GameId of(String raw) {
        return new GameId(raw);
    }

    public static GameId random() {
        return new GameId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return raw;
    }
}