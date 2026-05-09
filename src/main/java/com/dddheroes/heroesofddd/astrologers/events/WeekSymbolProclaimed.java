package com.dddheroes.heroesofddd.astrologers.events;

import com.dddheroes.heroesofddd.astrologers.write.AstrologersId;
import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;
import org.axonframework.eventsourcing.annotation.EventTag;
import org.axonframework.messaging.eventhandling.annotation.Event;

@Event
public record WeekSymbolProclaimed(
        @EventTag(key = "Astrologers")
        String astrologersId,
        Integer month,
        Integer week,
        String weekOf,
        Integer growth
) implements AstrologersEvent {

    public static WeekSymbolProclaimed event(
            AstrologersId astrologersId,
            MonthWeek monthWeek,
            WeekSymbol symbol
    ) {
        return new WeekSymbolProclaimed(
                astrologersId.raw(),
                monthWeek.month(),
                monthWeek.week(),
                symbol.weekOf().raw(),
                symbol.growth()
        );
    }
}
