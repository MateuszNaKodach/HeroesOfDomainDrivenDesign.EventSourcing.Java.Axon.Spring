package com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol;

import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;

import java.util.List;
import java.util.Random;

public class WeekSymbolCalculator {

    private static final List<String> CREATURE_SYMBOLS = List.of(
            "angel", "archangel", "peasant", "haste", "slow", "bless", "bloodlust", "dragon",
            "titan", "pixie", "air_elemental", "earth_elemental", "fire_elemental", "water_elemental"
    );

    private static final int DEFAULT_GROWTH = 5;
    private static final Random random = new Random();

    public CreatureId calculateWeekSymbol() {
        String symbol = CREATURE_SYMBOLS.get(random.nextInt(CREATURE_SYMBOLS.size()));
        return CreatureId.of(symbol);
    }

    public int calculateGrowth() {
        return DEFAULT_GROWTH;
    }
} 