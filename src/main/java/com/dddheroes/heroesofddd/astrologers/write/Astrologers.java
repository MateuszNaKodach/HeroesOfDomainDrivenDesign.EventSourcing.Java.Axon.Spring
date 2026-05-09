package com.dddheroes.heroesofddd.astrologers.write;

import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.OnlyOneSymbolPerWeek;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import com.dddheroes.heroesofddd.astrologers.events.WeekSymbolProclaimed;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;

@EventSourced(tagKey = "Astrologers", idType = AstrologersId.class)
class Astrologers {

    private AstrologersId astrologersId;
    private MonthWeek week;

    @CommandHandler
    void decide(ProclaimWeekSymbol command, EventAppender eventAppender) {
        new OnlyOneSymbolPerWeek(command, week).verify();

        eventAppender.append(WeekSymbolProclaimed.event(
                command.astrologersId(),
                command.week(),
                command.symbol()
        ));
    }

    @EventSourcingHandler
    void evolve(WeekSymbolProclaimed event) {
        this.astrologersId = new AstrologersId(event.astrologersId());
        this.week = new MonthWeek(event.month(), event.week());
    }

    @EntityCreator
    Astrologers() {
        // required by Axon
    }
}
