package com.dddheroes.heroesofddd.creaturerecruitment.write;

import java.util.UUID;

public record DwellingId(String raw) {

    public DwellingId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Dwelling id cannot be null or empty");
        }
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
}