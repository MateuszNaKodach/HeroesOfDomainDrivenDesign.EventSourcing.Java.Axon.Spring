package com.dddheroes.heroesofddd.astrologers;

import com.dddheroes.heroesofddd.astrologers.automation.whenweekstartedthenproclaimweeksymbol.WeekSymbolCalculator;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.eventsourcing.EventCountSnapshotTriggerDefinition;
import org.axonframework.eventsourcing.SnapshotTriggerDefinition;
import org.axonframework.eventsourcing.Snapshotter;
import org.axonframework.eventsourcing.snapshotting.SnapshotFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AstrologersConfiguration {

    // todo: add more options than angel
    @Bean
    WeekSymbolCalculator inMemoryWeekSymbolCalculator() {
        return __ -> WeekSymbol.of(CreatureId.of("angel"), random(1, 5));
    }

    private static int random(int min, int max) {
        return (int) (Math.random() * (max - min + 1) + min);
    }

    @Bean
    public SnapshotFilter astrologersSnapshotFilter() {
        return snapshotData -> {
            // Allow all snapshots for dwellings, as they are always in the correct format
            return true;
        };
    }

    @Bean
    SnapshotTriggerDefinition astrologersSnapshotTrigger(Snapshotter snapshotter) {
        return new EventCountSnapshotTriggerDefinition(snapshotter, 5);
    }
}
