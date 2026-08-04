package com.dddheroes.heroesofddd.creaturerecruitment.read.watchdwelling;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;

public record WatchDwelling(GameId gameId, DwellingId dwellingId) {

    public static WatchDwelling query(String gameId, String dwellingId) {
        return new WatchDwelling(GameId.of(gameId), DwellingId.of(dwellingId));
    }
}
