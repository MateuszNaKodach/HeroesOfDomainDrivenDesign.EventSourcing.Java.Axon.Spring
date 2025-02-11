package com.dddheroes.heroesofddd.calendar.write.startday;

import com.dddheroes.heroesofddd.calendar.write.Day;
import com.dddheroes.heroesofddd.calendar.write.Month;
import com.dddheroes.heroesofddd.calendar.write.Week;
import com.dddheroes.heroesofddd.shared.DomainRule;

public record CannotSkipDays(
        StartDay command,
        Month currentMonth,
        Week currentWeek,
        Day currentDay
) implements DomainRule {

    @Override
    public boolean isViolated() {
        return !isStartForTheNextDay();
    }

    @Override
    public String message() {
        return "Cannot skip days";
    }

    public boolean isStartForTheNextDay() {
        if (currentDay == null) {
            return true; // First day
        }

        int nextDay = currentDay.raw() + 1;
        int nextWeek = currentWeek.raw();
        int nextMonth = currentMonth.raw();

        if (nextDay > 7) {
            nextDay = 1;
            nextWeek += 1;
        }

        if (nextWeek > 4) {
            nextWeek = 1;
            nextMonth += 1;
        }

        return command.day().raw() == nextDay &&
                command.week().raw() == nextWeek &&
                command.month().raw() == nextMonth;
    }
}
