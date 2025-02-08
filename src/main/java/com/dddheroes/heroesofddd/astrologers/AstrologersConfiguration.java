package com.dddheroes.heroesofddd.astrologers;

import com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol.WeekSymbolCalculator;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;
import com.dddheroes.heroesofddd.shared.CreatureId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AstrologersConfiguration {

    // todo: add more options than angel
    @Bean
    WeekSymbolCalculator inMemoryWeekSymbolCalculator() {
        return __ -> WeekSymbol.of(CreatureId.of("angel"), random(1, 5));
    }

    private static int random(int min, int max) {
        return (int) (Math.random() * (max - min + 1) + min);
    }
}
