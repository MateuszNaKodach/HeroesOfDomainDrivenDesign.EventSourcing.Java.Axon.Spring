package com.dddheroes.heroesofddd.astrologers;

import com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol.WeekSymbolCalculator;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;
import com.dddheroes.heroesofddd.shared.CreatureId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AstrologersConfiguration {

    @Bean
    WeekSymbolCalculator inMemoryWeekSymbolCalculator() {
        return __ -> WeekSymbol.of(CreatureId.of("angel"), 1);
    }
}
