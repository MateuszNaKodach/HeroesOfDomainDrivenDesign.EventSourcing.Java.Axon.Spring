package com.dddheroes.heroesofddd.astrologers.write;

import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.OnlyOneSymbolPerWeek;
import com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol.ProclaimWeekSymbol;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import com.dddheroes.heroesofddd.astrologers.events.WeekSymbolProclaimed;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@EventSourced(tagKey = "Astrologers", idType = AstrologersId.class)
class Astrologers {

    private AstrologersId astrologersId;
    private MonthWeek week;

    static void decide(
            ProclaimWeekSymbol command,
            @Nullable Astrologers astrologers,
            EventAppender eventAppender
    ) {
        new OnlyOneSymbolPerWeek(command, astrologers == null ? null : astrologers.week).verify();

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

@Component
class AstrologersCommandHandler {

    @CommandHandler
    void decide(ProclaimWeekSymbol command, @InjectEntity @Nullable Astrologers astrologers, EventAppender eventAppender) {
        Astrologers.decide(command, astrologers, eventAppender);
    }
}
