package com.dddheroes.heroesofddd.calendar.write;

public record Week(Integer raw) {

    public Week {
        if (raw == null || raw < 1 || raw > 4) {
            throw new IllegalArgumentException("Week must be between 1 and 4");
        }
    }

    public static Week of(Integer raw) {
        return new Week(raw);
    }

}
