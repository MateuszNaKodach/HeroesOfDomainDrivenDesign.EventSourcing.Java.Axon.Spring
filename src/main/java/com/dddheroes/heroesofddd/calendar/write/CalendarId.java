package com.dddheroes.heroesofddd.calendar.write;

import java.util.UUID;

public record CalendarId(String raw) {

    public CalendarId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Calendar id cannot be null or empty");
        }
    }

    public static CalendarId of(String raw) {
        return new CalendarId(raw);
    }

    public static CalendarId random() {
        return new CalendarId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return "Calendar:" + raw;
    }
}