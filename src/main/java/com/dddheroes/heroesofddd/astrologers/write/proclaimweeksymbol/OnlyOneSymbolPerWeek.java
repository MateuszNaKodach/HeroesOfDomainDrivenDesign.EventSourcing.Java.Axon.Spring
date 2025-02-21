package com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol;

import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;

public record OnlyOneSymbolPerWeek(
        ProclaimWeekSymbol command,
        MonthWeek lastlyProclaimed
) implements DomainRule {

    @Override
    public boolean isViolated() {
        return lastlyProclaimed != null && lastlyProclaimed.weekNumber() >= command.week().weekNumber();
    }

    @Override
    public String message() {
        return "Only one symbol can be proclaimed per week";
    }
}
