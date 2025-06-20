package com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol;

import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;

import java.util.Set;

public class OnlyOneSymbolPerWeek implements DomainRule {

    private final ProclaimWeekSymbol command;
    private final Set<MonthWeek> proclaimedWeeks;

    public OnlyOneSymbolPerWeek(ProclaimWeekSymbol command, Set<MonthWeek> proclaimedWeeks) {
        this.command = command;
        this.proclaimedWeeks = proclaimedWeeks;
    }

    @Override
    public boolean isViolated() {
        return proclaimedWeeks.contains(command.monthWeek());
    }

    @Override
    public String message() {
        return "Only one symbol per week can be proclaimed";
    }
} 