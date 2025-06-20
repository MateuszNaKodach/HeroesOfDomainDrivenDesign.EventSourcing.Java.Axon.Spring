package com.dddheroes.heroesofddd.astrologers.write;

import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;

public record WeekSymbol(CreatureId weekOf, Integer growth) {

    public WeekSymbol {
        if (weekOf == null) {
            throw new IllegalArgumentException("WeekOf creature cannot be null");
        }
        if (growth == null) {
            throw new IllegalArgumentException("Growth cannot be null");
        }
    }

    public static WeekSymbol of(CreatureId weekOf, Integer growth) {
        return new WeekSymbol(weekOf, growth);
    }
} 