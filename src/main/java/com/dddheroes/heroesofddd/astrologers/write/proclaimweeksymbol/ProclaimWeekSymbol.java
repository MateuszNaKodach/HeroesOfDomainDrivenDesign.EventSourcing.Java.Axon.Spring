package com.dddheroes.heroesofddd.astrologers.write.proclaimweeksymbol;

import com.dddheroes.heroesofddd.astrologers.write.AstrologersCommand;
import com.dddheroes.heroesofddd.astrologers.write.AstrologersId;
import com.dddheroes.heroesofddd.astrologers.write.MonthWeek;
import com.dddheroes.heroesofddd.astrologers.write.WeekSymbol;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.messaging.commandhandling.annotation.Command;
import org.axonframework.modelling.annotation.TargetEntityId;

@Command
public record ProclaimWeekSymbol(
        @TargetEntityId
        AstrologersId astrologersId,
        MonthWeek week,
        WeekSymbol symbol
) implements AstrologersCommand {

    public static ProclaimWeekSymbol command(
            String astrologersId,
            Integer month,
            Integer week,
            String creatureId,
            Integer growth
    ) {
        return new ProclaimWeekSymbol(
                AstrologersId.of(astrologersId),
                MonthWeek.of(month, week),
                WeekSymbol.of(CreatureId.of(creatureId), growth)
        );
    }
}
