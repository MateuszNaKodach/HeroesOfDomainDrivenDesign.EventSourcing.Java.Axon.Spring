package com.dddheroes.heroesofddd.creaturerecruitment.write;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.events.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.OnlyNotBuiltBuildingCanBeBuild;
import com.dddheroes.heroesofddd.creaturerecruitment.events.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.IncreaseAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.OnlyBuiltDwellingCanHaveAvailableCreatures;
import com.dddheroes.heroesofddd.creaturerecruitment.events.CreatureRecruited;
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

import static org.axonframework.modelling.command.AggregateLifecycle.*;

@Aggregate
class Dwelling {

    @AggregateIdentifier
    private DwellingId dwellingId;
    private CreatureId creatureId;
    private Resources costPerTroop;
    private Amount availableCreatures;

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
    void evolve(CreatureRecruited event) {
        // todo: consider if it's OK or RecruitCreature should cause also AvailableCreaturesChanged event
        this.availableCreatures = this.availableCreatures.minus(new Amount(event.quantity()));
    }

    Dwelling() {
        // required by Axon
    }
}
