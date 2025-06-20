package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import com.dddheroes.heroesofddd.astrologers.events.WeekSymbolProclaimed;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.shared.application.GameMetaData;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.ReplayStatus;
import org.springframework.stereotype.Component;

@Component
@ProcessingGroup("Automation_WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreatures_Processor")
public class WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor {

    private final CommandGateway commandGateway;
    private final BuiltDwellingReadModelRepository dwellingRepository;

    public WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor(
            CommandGateway commandGateway,
            BuiltDwellingReadModelRepository dwellingRepository) {
        this.commandGateway = commandGateway;
        this.dwellingRepository = dwellingRepository;
    }

    @EventHandler
    public void on(WeekSymbolProclaimed event, ReplayStatus replayStatus) {
        if (replayStatus.isReplay()) {
            return; // Do not process replayed events
        }

        // Find all dwellings that produce the creature of the week
        var matchingDwellings = dwellingRepository.findAllByCreatureId(event.weekOf());

        // Send IncreaseAvailableCreatures command for each matching dwelling
        matchingDwellings.forEach(dwelling -> {
            var command = IncreaseAvailableCreatures.command(
                    dwelling.dwellingId(),
                    event.weekOf(),
                    event.growth()
            );
            commandGateway.sendAndWait(command, GameMetaData.with(event.astrologersId()));
        });
    }
} 