package com.dddheroes.heroesofddd.calendar.write;

public record Month(Integer raw) {

    public Month {
        if (raw == null || raw < 1) {
            throw new IllegalArgumentException("Month must be greater than 0");
        }
    }

    public static Month of(Integer raw) {
        return new Month(raw);
    }

}
