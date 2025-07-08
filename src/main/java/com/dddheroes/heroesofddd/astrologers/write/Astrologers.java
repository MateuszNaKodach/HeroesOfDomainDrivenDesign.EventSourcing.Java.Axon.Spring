package com.dddheroes.heroesofddd.astrologers.write;

import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.OnlyOneSymbolPerWeek;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import com.dddheroes.heroesofddd.astrologers.events.WeekSymbolProclaimed;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate(snapshotFilter = "astrologersSnapshotFilter", snapshotTriggerDefinition = "astrologersSnapshotTrigger")
class Astrologers {

    @AggregateIdentifier
    private AstrologersId astrologersId;
    private MonthWeek week;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    void decide(ProclaimWeekSymbol command) {
        new OnlyOneSymbolPerWeek(command, week).verify();

        apply(
                WeekSymbolProclaimed.event(
                        command.astrologersId(),
                        command.week(),
                        command.symbol()
                )
        );
    }

    @EventSourcingHandler
    void evolve(WeekSymbolProclaimed event) {
        this.astrologersId = new AstrologersId(event.astrologersId());
        this.week = new MonthWeek(event.month(), event.week());
    }

    Astrologers() {
        // required by Axon
    }
}
