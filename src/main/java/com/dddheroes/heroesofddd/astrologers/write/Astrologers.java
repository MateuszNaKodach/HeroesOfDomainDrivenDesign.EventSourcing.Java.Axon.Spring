package com.dddheroes.heroesofddd.astrologers.write;

import com.dddheroes.heroesofddd.astrologers.events.WeekSymbolProclaimed;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.OnlyOneSymbolPerWeek;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.HashSet;
import java.util.Set;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
class Astrologers {

    @AggregateIdentifier
    private AstrologersId astrologersId;
    private Set<MonthWeek> proclaimedWeeks;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    void decide(ProclaimWeekSymbol command) {
        new OnlyOneSymbolPerWeek(command, proclaimedWeeks != null ? proclaimedWeeks : Set.of()).verify();

        apply(
                WeekSymbolProclaimed.event(
                        command.astrologersId(),
                        command.monthWeek(),
                        command.weekSymbol()
                )
        );
    }

    @EventSourcingHandler
    void evolve(WeekSymbolProclaimed event) {
        this.astrologersId = AstrologersId.of(event.astrologersId());
        if (this.proclaimedWeeks == null) {
            this.proclaimedWeeks = new HashSet<>();
        }
        this.proclaimedWeeks.add(MonthWeek.of(event.month(), event.week()));
    }

    Astrologers() {
        // required by Axon
    }
} 