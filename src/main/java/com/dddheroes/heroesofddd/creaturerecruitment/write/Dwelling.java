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
import com.dddheroes.heroesofddd.shared.domain.identifiers.CreatureId;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.axonframework.modelling.command.AggregateLifecycle.*;

@Aggregate(snapshotTriggerDefinition = "dwellingSnapshotTrigger", snapshotFilter = "dwellingSnapshotFilter")
public class Dwelling {

    private static final Logger logger = LoggerFactory.getLogger(Dwelling.class);

    @AggregateIdentifier
    public DwellingId dwellingId;
    public CreatureId creatureId;
    public Resources costPerTroop;
    public Amount availableCreatures;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING) // performance downside in comparison to constructor
    void decide(BuildDwelling command) {
        new OnlyNotBuiltBuildingCanBeBuild(dwellingId).verify();

        apply(
                DwellingBuilt.event(
                        command.dwellingId(),
                        command.creatureId(),
                        command.costPerTroop()
                )
        );
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
    void decide(IncreaseAvailableCreatures command) {
        new OnlyBuiltDwellingCanHaveAvailableCreatures(dwellingId).verify();
        // todo: check creatureId for the dwelling!

        apply(
                AvailableCreaturesChanged.event(
                        command.dwellingId(),
                        command.creatureId(),
                        availableCreatures.plus(command.increaseBy())
                )
        );
    }

    @EventSourcingHandler
    void evolve(AvailableCreaturesChanged event) {
        logger.info("📈 Available creatures changed for dwelling {}: {} creatures now available",
                event.dwellingId(), event.changedTo());
        this.availableCreatures = new Amount(event.changedTo());
    }

    @CommandHandler
//    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    void decide(RecruitCreature command) {
//        if(dwellingId == null){
//            throw new DomainRule.ViolatedException("Only not built building can be build");
//        }
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

        apply(
                CreatureRecruited.event(
                        command.dwellingId(),
                        command.creatureId(),
                        command.toArmy(),
                        command.quantity(),
                        recruitCost
                )
        );
    }

    @EventSourcingHandler
    void evolve(CreatureRecruited event) {
        logger.info("🧙 Recruited {} creatures of type {} from dwelling {} to army {}",
                event.quantity(), event.creatureId(), event.dwellingId(), event.toArmy());
        // todo: consider if it's OK or RecruitCreature should cause also AvailableCreaturesChanged event
        this.availableCreatures = this.availableCreatures.minus(new Amount(event.quantity()));
    }

    Dwelling() {
        logger.info("🏠 Creating empty Dwelling (required by Axon)");
        // required by Axon
    }
}
