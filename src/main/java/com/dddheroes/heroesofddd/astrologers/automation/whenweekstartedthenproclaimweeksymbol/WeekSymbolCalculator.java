package com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol;

import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;

import java.util.function.Function;

@FunctionalInterface
public interface WeekSymbolCalculator extends Function<MonthWeek, WeekSymbol> {

}
