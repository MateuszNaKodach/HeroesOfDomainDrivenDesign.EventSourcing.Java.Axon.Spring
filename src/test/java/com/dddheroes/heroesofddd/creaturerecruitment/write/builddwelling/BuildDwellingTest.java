package com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingTest;
import com.dddheroes.heroesofddd.shared.domain.DomainRule;
import org.junit.jupiter.api.*;

import java.util.List;

class BuildDwellingTest extends DwellingTest {

    @Test
    void givenNotBuiltDwellingWhenBuildThenBuilt() {
        // given
        var givenEvents = List.of();

        // when
        var whenCommand = buildDwelling();

        // then
        var thenEvent = dwellingBuilt();
        fixture.given().events(givenEvents)
               .when().command(whenCommand)
               .then().events(thenEvent);
    }

    @Test
    void givenBuiltDwellingWhenBuildSameOneMoreTimeThenException() {
        // given
        var givenEvents = List.of(
                dwellingBuilt()
        );

        // when
        var whenCommand = buildDwelling();

        // then
        fixture.given().events(givenEvents)
               .when().command(whenCommand)
               .then().exception(DomainRule.ViolatedException.class, "Only not built building can be build");
    }
}
