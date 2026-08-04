package com.dddheroes.heroesofddd.creaturerecruitment.read.streamdwellings;

import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;

public record StreamDwellings(GameId gameId) {

    public static StreamDwellings query(String gameId) {
        return new StreamDwellings(GameId.of(gameId));
    }
}
