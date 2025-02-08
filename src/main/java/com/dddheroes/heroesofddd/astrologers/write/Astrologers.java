package com.dddheroes.heroesofddd.astrologers.write;

import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
class Astrologers {

    @AggregateIdentifier
    private AstrologersId astrologersId;
    private MonthWeek week;
    private WeekSymbol symbol;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    void decide(ProclaimWeekSymbol command) {
        apply(
                null
        );
    }

    Astrologers() {
        // required by Axon
    }
}
