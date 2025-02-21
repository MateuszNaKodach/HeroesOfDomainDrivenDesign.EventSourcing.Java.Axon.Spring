package com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;

public record GetDwellingById(GameId gameId, DwellingId dwellingId) {

    public static GetDwellingById query(String gameId, String dwellingId) {
        return new GetDwellingById(GameId.of(gameId), DwellingId.of(dwellingId));
    }

}
