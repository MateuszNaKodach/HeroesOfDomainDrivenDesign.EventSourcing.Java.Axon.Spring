package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.WeekSymbolProclaimed;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.SequenceNumber;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.stereotype.Component;

@ProcessingGroup("Automation_WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreatures_Processor")
@DisallowReplay
@Component
class WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor {

    private final EventStore eventStore;

    WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    @EventHandler
    void react(WeekSymbolProclaimed event, @SequenceNumber long sequenceNumber) {
        // todo: separate dwelling per game. Now we read all of them
        // I want be consistent here. With DBC it'd be nice to query all types and by tags like game.
        // use EventStore

    }

}
