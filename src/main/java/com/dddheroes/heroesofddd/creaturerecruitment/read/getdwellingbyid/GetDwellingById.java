package com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;

public record GetDwellingById(DwellingId dwellingId) {

    public static GetDwellingById query(String dwellingId) {
        return new GetDwellingById(DwellingId.of(dwellingId));
    }

}
