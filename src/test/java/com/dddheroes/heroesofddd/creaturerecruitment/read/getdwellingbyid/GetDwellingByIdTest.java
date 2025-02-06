package com.dddheroes.heroesofddd.creaturerecruitment.read.getdwellingbyid;

import com.dddheroes.heroesofddd.TestcontainersConfiguration;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GetDwellingByIdTest {

    @Autowired
    private QueryGateway queryGateway;

    @Autowired
    private EventGateway eventGateway;

    @Test
    void dwellingNotExist() {
        // when
        var result = queryGateway.query(GetDwellingById.query("1"), DwellingReadModel.class).join();

        // then
        assertThat(result).isNull();
    }

    @Test
    void dwellingExist() {
        // given
        eventGateway.publish(new DwellingBuilt("2", "2", new HashMap<>()));

        // when
        var result = queryGateway.query(GetDwellingById.query("2"), DwellingReadModel.class).join();

        // then
        assertThat(result).isNotNull();
    }
}