package com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling;

import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingTest;
import com.dddheroes.heroesofddd.shared.DomainRule;
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
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectEvents(thenEvent);
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
        fixture.given(givenEvents)
               .when(whenCommand)
               .expectException(DomainRule.ViolatedException.class)
               .expectExceptionMessage("Only not built building can be build");
    }
}