package com.dddheroes.heroesofddd.calendar.write.finishday;

import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;
import com.dddheroes.heroesofddd.shared.DomainRule;

public record CanOnlyFinishCurrentDay(
        FinishDay command,
        Month currentMonth,
        Week currentWeek,
        Day currentDay
) implements DomainRule {

    @Override
    public boolean isViolated() {
        boolean isCurrentDay = command.month().equals(currentMonth)
                && command.week().equals(currentWeek)
                && command.day().equals(currentDay);
        return !isCurrentDay;
    }

    @Override
    public String message() {
        return "Can only finish current day";
    }
}
