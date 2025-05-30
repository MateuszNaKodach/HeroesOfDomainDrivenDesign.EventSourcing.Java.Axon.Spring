package com.dddheroes.heroesofddd.astrologers.write;

public record MonthWeek(Integer month, Integer week) {

    public static MonthWeek of(Integer month, Integer week) {
        // todo: validation
        return new MonthWeek(month, week);
    }

    public int weekNumber() {
        return ((month - 1) * 4) + week;
    }
}