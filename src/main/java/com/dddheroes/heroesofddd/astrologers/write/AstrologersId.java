package com.dddheroes.heroesofddd.astrologers.write;

import java.util.UUID;

public record AstrologersId(String raw) {

    private final static String AGGREGATE_TYPE = "Astrologers";

    public AstrologersId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Astrologers id cannot be null or empty");
        }
        raw = withType(raw);
    }

    public static AstrologersId of(String raw) {
        return new AstrologersId(raw);
    }

    public static AstrologersId random() {
        return new AstrologersId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return raw;
    }

    private static String withType(String id) {
        return id.startsWith(AGGREGATE_TYPE + ":") ? id : AGGREGATE_TYPE + ":" + id;
    }
} 