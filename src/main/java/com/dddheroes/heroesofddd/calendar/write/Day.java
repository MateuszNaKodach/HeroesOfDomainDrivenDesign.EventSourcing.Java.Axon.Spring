package com.dddheroes.heroesofddd.calendar.write;

public record Day(Integer raw) {

    public Day {
        if (raw == null || raw < 1 || raw > 7) {
            throw new IllegalArgumentException("Day must be between 1 and 7");
        }
    }

    public static Day of(Integer raw) {
        return new Day(raw);
    }

}
