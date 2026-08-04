package com.dddheroes.heroesofddd.creaturerecruitment.write;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.OnlyNotBuiltBuildingCanBeBuild;
import com.dddheroes.heroesofddd.creaturerecruitment.events.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.OnlyBuiltDwellingCanHaveAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCostCannotDifferThanExpectedCost;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreaturesNotExceedAvailableCreatures;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Amount;
import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.eventsourcing.annotation.Snapshotting;
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.axonframework.modelling.annotation.InjectEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@EventSourced(tagKey = "Dwelling", idType = DwellingId.class)
@Snapshotting(afterEvents = 3)
public class Dwelling {

    private static final Logger logger = LoggerFactory.getLogger(Dwelling.class);

    public DwellingId dwellingId; // needs to be public for snapshotting
    public CreatureId creatureId;
    public Resources costPerTroop;
    public Amount availableCreatures;

    static void decide(
            BuildDwelling command,
            @Nullable Dwelling dwelling,
            EventAppender eventAppender
    ) {
        new OnlyNotBuiltBuildingCanBeBuild(dwelling == null ? null : dwelling.dwellingId).verify();

        eventAppender.append(DwellingBuilt.event(
                command.dwellingId(),
                command.creatureId(),
                command.costPerTroop()
        ));
    }

    @EventSourcingHandler
    void evolve(DwellingBuilt event) {
        logger.info("🏗️ Dwelling built with ID: {}, creature type: {}", event.dwellingId(), event.creatureId());
        this.dwellingId = new DwellingId(event.dwellingId());
        this.creatureId = new CreatureId(event.creatureId());
        this.costPerTroop = Resources.fromRaw(event.costPerTroop());
        this.availableCreatures = Amount.zero();
    }

    static void decide(
            IncreaseAvailableCreatures command,
            @Nullable Dwelling dwelling,
            EventAppender eventAppender
    ) {
        new OnlyBuiltDwellingCanHaveAvailableCreatures(dwelling == null ? null : dwelling.dwellingId).verify();
        // todo: check creatureId for the dwelling!

        eventAppender.append(AvailableCreaturesChanged.event(
                command.dwellingId(),
                command.creatureId(),
                dwelling.availableCreatures.plus(command.increaseBy())
        ));
    }

    @EventSourcingHandler
    void evolve(AvailableCreaturesChanged event) {
        logger.info("📈 Available creatures changed for dwelling {}: {} creatures now available",
                event.dwellingId(), event.changedTo());
        this.availableCreatures = new Amount(event.changedTo());
    }

    static void decide(
            RecruitCreature command,
            @Nullable Dwelling dwelling,
            EventAppender eventAppender
    ) {
        new OnlyBuiltDwellingCanHaveAvailableCreatures(dwelling == null ? null : dwelling.dwellingId).verify();

        new RecruitCreaturesNotExceedAvailableCreatures(
                dwelling.creatureId,
                dwelling.availableCreatures,
                command.creatureId(),
                command.quantity()
        ).verify();

        var recruitCost = dwelling.costPerTroop.multiply(command.quantity());
        new RecruitCostCannotDifferThanExpectedCost(
                recruitCost,
                command.expectedCost()
        ).verify();

        eventAppender.append(CreatureRecruited.event(
                command.dwellingId(),
                command.creatureId(),
                command.toArmy(),
                command.quantity(),
                recruitCost
        ));
    }

    @EventSourcingHandler
    void evolve(CreatureRecruited event) {
        logger.info("🧙 Recruited {} creatures of type {} from dwelling {} to army {}",
                event.quantity(), event.creatureId(), event.dwellingId(), event.toArmy());
        // todo: consider if it's OK or RecruitCreature should cause also AvailableCreaturesChanged event
        this.availableCreatures = this.availableCreatures.minus(new Amount(event.quantity()));
    }

    @EntityCreator
    Dwelling() {
        logger.info("\uD83D\uDC80 Dwelling non-args constructor");
        // required by Axon
    }


}

@Component
class DwellingCommandHandler {

    @CommandHandler
    void decide(BuildDwelling command, @InjectEntity @Nullable Dwelling dwelling, EventAppender eventAppender) {
        Dwelling.decide(command, dwelling, eventAppender);
    }

    @CommandHandler
    void decide(IncreaseAvailableCreatures command, @InjectEntity @Nullable Dwelling dwelling, EventAppender eventAppender) {
        Dwelling.decide(command, dwelling, eventAppender);
    }

    @CommandHandler
    void decide(RecruitCreature command, @InjectEntity @Nullable Dwelling dwelling, EventAppender eventAppender) {
        Dwelling.decide(command, dwelling, eventAppender);
    }
}
