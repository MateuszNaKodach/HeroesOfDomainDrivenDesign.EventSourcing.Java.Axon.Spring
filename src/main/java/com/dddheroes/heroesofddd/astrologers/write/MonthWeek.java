package com.dddheroes.heroesofddd.astrologers.write;

public record MonthWeek(Integer month, Integer week) {

    public MonthWeek {
        if (month == null || month <= 0) {
            throw new IllegalArgumentException("Month must be positive");
        }
        if (week == null || week <= 0) {
            throw new IllegalArgumentException("Week must be positive");
        }
    }

    public static MonthWeek of(Integer month, Integer week) {
        return new MonthWeek(month, week);
    }
} 