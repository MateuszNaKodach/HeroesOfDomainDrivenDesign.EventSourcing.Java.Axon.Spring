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
import org.axonframework.eventsourcing.annotation.reflection.EntityCreator;
import org.axonframework.extension.spring.stereotype.EventSourced;
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.axonframework.messaging.eventhandling.gateway.EventAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO #LLM: reconfigure snapshot trigger (AF4 had snapshotTriggerDefinition = "dwellingSnapshotTrigger")
@EventSourced(tagKey = "Dwelling", idType = DwellingId.class)
public class Dwelling {

    private static final Logger logger = LoggerFactory.getLogger(Dwelling.class);

    public DwellingId dwellingId; // needs to be public for snapshotting
    public CreatureId creatureId;
    public Resources costPerTroop;
    public Amount availableCreatures;

    @CommandHandler // performance downside in comparison to constructor
    void decide(BuildDwelling command, EventAppender eventAppender) {
        new OnlyNotBuiltBuildingCanBeBuild(dwellingId).verify();

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

    @CommandHandler
    void decide(IncreaseAvailableCreatures command, EventAppender eventAppender) {
        new OnlyBuiltDwellingCanHaveAvailableCreatures(dwellingId).verify();
        // todo: check creatureId for the dwelling!

        eventAppender.append(AvailableCreaturesChanged.event(
                command.dwellingId(),
                command.creatureId(),
                availableCreatures.plus(command.increaseBy())
        ));
    }

    @EventSourcingHandler
    void evolve(AvailableCreaturesChanged event) {
        logger.info("📈 Available creatures changed for dwelling {}: {} creatures now available",
                event.dwellingId(), event.changedTo());
        this.availableCreatures = new Amount(event.changedTo());
    }

    @CommandHandler
    void decide(RecruitCreature command, EventAppender eventAppender) {
        // AF5: with @EntityCreator no-arg, the framework materialises an empty Dwelling
        // before this handler runs. Guard against the not-yet-built case explicitly so we
        // surface the domain rule instead of NPE-ing on null state.
        new OnlyBuiltDwellingCanHaveAvailableCreatures(dwellingId).verify();

        new RecruitCreaturesNotExceedAvailableCreatures(
                creatureId,
                availableCreatures,
                command.creatureId(),
                command.quantity()
        ).verify();

        var recruitCost = costPerTroop.multiply(command.quantity());
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
