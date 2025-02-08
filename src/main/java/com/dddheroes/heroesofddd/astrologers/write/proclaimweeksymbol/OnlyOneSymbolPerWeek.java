package com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol;

import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;
import com.dddheroes.heroesofddd.calendar.write.finishday.FinishDay;
import com.dddheroes.heroesofddd.shared.DomainRule;

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
