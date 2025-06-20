package com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol;

import com.dddheroes.heroesofddd.astrologers.write.AstrologersCommand;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersId;
import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record ProclaimWeekSymbol(
        @TargetAggregateIdentifier
        AstrologersId astrologersId,
        MonthWeek monthWeek,
        WeekSymbol weekSymbol
) implements AstrologersCommand {

    public static ProclaimWeekSymbol command(String astrologersId, Integer month, Integer week, String weekOf, Integer growth) {
        return new ProclaimWeekSymbol(
                AstrologersId.of(astrologersId),
                MonthWeek.of(month, week),
                WeekSymbol.of(CreatureId.of(weekOf), growth)
        );
    }
} 