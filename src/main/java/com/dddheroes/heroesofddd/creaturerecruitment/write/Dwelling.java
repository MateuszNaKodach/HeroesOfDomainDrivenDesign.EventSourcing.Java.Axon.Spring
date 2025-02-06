package com.dddheroes.heroesofddd.creaturerecruitment.write;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.OnlyNotBuiltBuildingCanBeBuild;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.OnlyBuiltDwellingCanHaveAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.CreatureRecruited;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreature;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreaturesNotExceedAvailableCreatures;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.Cost;
import com.dddheroes.heroesofddd.shared.CreatureId;
import com.dddheroes.heroesofddd.shared.DomainRule;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.*;

@Aggregate
class Dwelling {

    @AggregateIdentifier
    private DwellingId dwellingId;
    private CreatureId creatureId;
    private Cost costPerTroop;
    private Amount availableCreatures;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING) // performance downside in comparison to constructor
    void handle(BuildDwelling command) {
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
    void on(DwellingBuilt event) {
        this.dwellingId = new DwellingId(event.dwellingId());
        this.creatureId = new CreatureId(event.creatureId());
        this.costPerTroop = Cost.fromRaw(event.costPerTroop());
        this.availableCreatures = Amount.zero();
    }

    @CommandHandler
    void handle(IncreaseAvailableCreatures command) {
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
    void on(AvailableCreaturesChanged event) {
        this.availableCreatures = new Amount(event.changedTo());
    }

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    void handle(RecruitCreature command) {
        if(dwellingId == null){
            throw new DomainRule.ViolatedException("Only not built building can be build");
        }
        new RecruitCreaturesNotExceedAvailableCreatures(
                creatureId,
                availableCreatures,
                command.creatureId(),
                command.quantity()
        ).verify();

        apply(
                CreatureRecruited.event(
                        command.dwellingId(),
                        command.creatureId(),
                        command.toArmy(),
                        command.quantity(),
                        costPerTroop.multiply(command.quantity())
                )
        );
    }

    @EventSourcingHandler
    void on(CreatureRecruited event) {
        // todo: consider if it's OK or RecruitCreature should cause also AvailableCreaturesChanged event
        this.availableCreatures = this.availableCreatures.minus(new Amount(event.quantity()));
    }

    Dwelling() {
        // required by Axon
    }
}
