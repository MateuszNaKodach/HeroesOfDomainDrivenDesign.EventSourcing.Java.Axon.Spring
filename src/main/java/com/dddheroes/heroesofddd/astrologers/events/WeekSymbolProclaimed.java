package com.dddheroes.heroesofddd.astrologers.events;

import com.dddheroes.heroesofddd.astrologers.write.AstrologersId;
import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;

public record WeekSymbolProclaimed(
        String astrologersId,
        Integer month,
        Integer week,
        String weekOf,
        Integer growth
) implements AstrologersEvent {

    public static WeekSymbolProclaimed event(AstrologersId astrologersId, MonthWeek monthWeek, WeekSymbol weekSymbol) {
        return new WeekSymbolProclaimed(
                astrologersId.raw(),
                monthWeek.month(),
                monthWeek.week(),
                weekSymbol.weekOf().raw(),
                weekSymbol.growth()
        );
    }
} 