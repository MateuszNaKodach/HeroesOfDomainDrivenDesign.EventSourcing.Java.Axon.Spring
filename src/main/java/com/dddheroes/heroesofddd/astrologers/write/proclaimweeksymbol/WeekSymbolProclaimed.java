package com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol;

import com.dddheroes.heroesofddd.astrologers.write.AstrologersEvent;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersId;
import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;

public record WeekSymbolProclaimed(
        String astrologersId,
        Integer month,
        Integer day,
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
