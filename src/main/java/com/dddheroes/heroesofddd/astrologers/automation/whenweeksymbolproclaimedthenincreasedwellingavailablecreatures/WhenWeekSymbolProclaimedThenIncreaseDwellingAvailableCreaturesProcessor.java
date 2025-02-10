package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.WeekSymbolProclaimed;
import com.dddheroes.heroesofddd.creaturerecruitment.read.DwellingReadModel;
import com.dddheroes.heroesofddd.creaturerecruitment.read.getalldwellings.GetAllDwellings;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DisallowReplay;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.SequenceNumber;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Component;

@ProcessingGroup("Automation_WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreatures_Processor")
@DisallowReplay
@Component
class WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor {

    private final QueryGateway queryGateway;
    private final CommandGateway commandGateway;

    WhenWeekSymbolProclaimedThenIncreaseDwellingAvailableCreaturesProcessor(
            QueryGateway queryGateway,
            CommandGateway commandGateway
    ) {
        this.queryGateway = queryGateway;
        this.commandGateway = commandGateway;
    }

    @EventHandler
    void react(WeekSymbolProclaimed event) {
        // todo: separate dwelling per game. Now we read all of them
        // I want be consistent here. With DBC it'd be nice to query all types and by tags like game.
        // use EventStore, @SequenceNumber long sequenceNumber

        var creature = event.weekOf();
        var increaseBy = event.growth();
        var toProcess = queryGateway.query(GetAllDwellings.query(), GetAllDwellings.Result.class);
        toProcess.thenAccept(r -> r.dwellings()
                                   .stream().filter(dwelling -> dwelling.getCreatureId().equals(creature))
                                   .forEach(dwelling -> increaseAvailableCreatures(dwelling, increaseBy)));
    }

    private void increaseAvailableCreatures(DwellingReadModel dwelling, Integer increaseBy) {
        var command = IncreaseAvailableCreatures.command(
                dwelling.getDwellingId(),
                dwelling.getCreatureId(),
                increaseBy
        );
        commandGateway.send(command);
    }
}
