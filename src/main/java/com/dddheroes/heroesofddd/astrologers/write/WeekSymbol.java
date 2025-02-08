package com.dddheroes.heroesofddd.astrologers.write;

import com.dddheroes.heroesofddd.shared.CreatureId;

// todo: polymorphism, support symbols which are not creatures
public record WeekSymbol(CreatureId creatureId, Integer growth) {

    public static WeekSymbol of(CreatureId creatureId, Integer growth) {
        return new WeekSymbol(creatureId, growth);
    }
}
