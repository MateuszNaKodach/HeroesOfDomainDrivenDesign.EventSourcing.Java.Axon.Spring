package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@ProcessingGroup("Automation_WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreatures_Processor")
public class BuiltDwellingReadModelRepository {

    private final List<BuiltDwellingReadModel> dwellings = new CopyOnWriteArrayList<>();

    @EventHandler
    public void on(DwellingBuilt event) {
        dwellings.add(new BuiltDwellingReadModel(event.dwellingId(), event.creatureId()));
    }

    public List<BuiltDwellingReadModel> findAllByCreatureId(String creatureId) {
        return dwellings.stream()
                .filter(dwelling -> dwelling.creatureId().equals(creatureId))
                .toList();
    }
} 