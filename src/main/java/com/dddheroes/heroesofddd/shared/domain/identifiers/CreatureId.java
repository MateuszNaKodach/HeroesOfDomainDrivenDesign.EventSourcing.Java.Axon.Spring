package com.dddheroes.heroesofddd.shared.domain.identifiers;

public record CreatureId(String raw) {

    public CreatureId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Dwelling id cannot be null or empty");
        }
    }

    public static CreatureId of(String raw) {
        return new CreatureId(raw);
    }

    @Override
    public String toString() {
        return raw;
    }
}