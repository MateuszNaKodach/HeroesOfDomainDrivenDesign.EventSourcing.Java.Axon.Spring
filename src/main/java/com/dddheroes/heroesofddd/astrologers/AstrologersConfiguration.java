package com.dddheroes.heroesofddd.astrologers;

import com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol.WeekSymbolCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AstrologersConfiguration {

    @Bean
    public WeekSymbolCalculator weekSymbolCalculator() {
        return new WeekSymbolCalculator();
    }
} 