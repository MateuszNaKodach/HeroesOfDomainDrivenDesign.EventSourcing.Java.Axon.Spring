package com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings;

import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;

import java.util.List;

public record GetAllDwellings() {

    public static GetAllDwellings query() {
        return new GetAllDwellings();
    }

    public record Result(List<DwellingReadModel> dwellings) {

    }
}
