package com.dddheroes.heroesofddd.astrologers.write;

import com.dddheroes.heroesofddd.shared.CreatureId;

// todo: polymorphism, support symbols which are not creatures
public record WeekSymbol(CreatureId weekOf, Integer growth) {

    public static WeekSymbol of(CreatureId weekOf, Integer growth) {
        return new WeekSymbol(weekOf, growth);
    }
}
