package com.dddheroes.heroesofddd.creaturerecruitment.read.listdwellings;

import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;

public record ListDwellings(GameId gameId) {

    public static ListDwellings query(String gameId) {
        return new ListDwellings(GameId.of(gameId));
    }
}
