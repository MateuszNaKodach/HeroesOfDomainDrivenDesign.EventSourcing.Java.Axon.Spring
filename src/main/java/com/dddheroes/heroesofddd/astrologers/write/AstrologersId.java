package com.dddheroes.heroesofddd.astrologers.write;

import java.util.UUID;

public record AstrologersId(String raw) {

    public AstrologersId {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Astrologers id cannot be null or empty");
        }
    }

    public static AstrologersId of(String raw) {
        return new AstrologersId(raw);
    }

    public static AstrologersId random() {
        return new AstrologersId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return "Astrologers:" + raw;
    }
}