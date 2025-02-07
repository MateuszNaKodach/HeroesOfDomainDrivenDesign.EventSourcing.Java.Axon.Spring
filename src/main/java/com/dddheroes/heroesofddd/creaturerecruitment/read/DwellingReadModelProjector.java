package com.dddheroes.heroesofddd.creaturerecruitment.read;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.CreatureRecruited;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

@ProcessingGroup("ReadModel_Dwelling")
@Component
class DwellingReadModelProjector {

    private final DwellingReadModelRepository repository;

    DwellingReadModelProjector(DwellingReadModelRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    void on(DwellingBuilt event) {
        var state = new DwellingReadModel(
                event.dwellingId(),
                event.creatureId(),
                event.costPerTroop(),
                0
        );
        repository.save(state);
    }

    @EventHandler
    void on(AvailableCreaturesChanged event) {
        repository.findById(event.dwellingId())
                  .map(state -> state.withAvailableCreatures(event.changedTo()))
                  .ifPresent(repository::save);
    }

    @EventHandler
    void on(CreatureRecruited event) {
        repository.findById(event.dwellingId())
                  .map(state -> state.withAvailableCreaturesDecreasedBy(event.quantity()))
                  .ifPresent(repository::save);
    }
}
