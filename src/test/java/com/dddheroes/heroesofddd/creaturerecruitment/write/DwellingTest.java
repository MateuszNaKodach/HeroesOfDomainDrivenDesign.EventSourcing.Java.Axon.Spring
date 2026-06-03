package com.dddheroes.heroesofddd.creaturerecruitment.write;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.eventsourcing.configuration.EventSourcedEntityModule;
import org.axonframework.eventsourcing.configuration.EventSourcingConfigurer;
import org.axonframework.eventsourcing.snapshot.inmemory.InMemorySnapshotStore;
import org.axonframework.eventsourcing.snapshot.store.SnapshotStore;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.ResourceType;
import org.axonframework.test.fixture.AxonTestFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class DwellingTest {

    protected final DwellingId dwellingId = DwellingId.random();
    protected final CreatureId angelId = CreatureId.of("angel");
    protected final Resources costPerTroop = Resources
            .from(ResourceType.GOLD, Amount.of(3000))
            .plus(ResourceType.GEMS, Amount.of(1));

    protected AxonTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = AxonTestFixture.with(
                EventSourcingConfigurer.create()
                                       // Dwelling declares @Snapshotting, which requires a SnapshotStore component.
                                       .componentRegistry(cr -> cr.registerComponent(
                                               SnapshotStore.class, c -> new InMemorySnapshotStore()))
                                       .registerEntity(EventSourcedEntityModule.autodetected(DwellingId.class,
                                                                                             Dwelling.class))
        );
    }

    @AfterEach
    void tearDown() {
        fixture.stop();
    }

    protected DwellingBuilt dwellingBuilt() {
        return DwellingBuilt.event(dwellingId, angelId, costPerTroop);
    }

    protected BuildDwelling buildDwelling() {
        return BuildDwelling.command(dwellingId.raw(), angelId.raw(), costPerTroop.raw());
    }
}