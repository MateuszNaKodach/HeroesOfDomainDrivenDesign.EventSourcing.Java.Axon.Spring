package com.dddheroes.heroesofddd.calendar.write;

import java.util.UUID;

public record CalendarId(String raw) {

    private final static String AGGREGATE_TYPE = "Calendar";

    public CalendarId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Calendar id cannot be null or empty");
        }
        raw = withType(raw);
    }

    public static CalendarId of(String raw) {
        return new CalendarId(raw);
    }

    public static CalendarId random() {
        return new CalendarId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return raw;
    }

    private static String withType(String id) {
        return id.startsWith(AGGREGATE_TYPE + ":") ? id : AGGREGATE_TYPE + ":" + id;
    }
}