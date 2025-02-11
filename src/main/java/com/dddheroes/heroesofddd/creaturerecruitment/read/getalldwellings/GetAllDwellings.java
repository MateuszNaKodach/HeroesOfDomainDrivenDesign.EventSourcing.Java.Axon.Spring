package com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.shared.GameId;

import java.util.List;

public record GetAllDwellings(GameId gameId) {

    public static GetAllDwellings query(String gameId) {
        return new GetAllDwellings(GameId.of(gameId));
    }

    public record Result(List<DwellingReadModel> dwellings) {

    }
}
