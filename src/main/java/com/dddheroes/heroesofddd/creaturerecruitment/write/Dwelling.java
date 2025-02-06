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
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import static org.axonframework.modelling.command.AggregateLifecycle.*;

@Aggregate
public class Dwelling {

    @AggregateIdentifier
    private DwellingId dwellingId;
    private CreatureId creatureId;
    private Cost costPerTroop;
    private Amount availableCreatures;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    void handle(BuildDwelling command) {
        new OnlyNotBuiltBuildingCanBeBuild(dwellingId).verify();

        apply(
                new DwellingBuilt(
                        command.dwellingId().raw(),
                        command.creatureId().raw(),
                        command.costPerTroop().raw()
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
                new AvailableCreaturesChanged(
                        command.dwellingId().raw(),
                        command.creatureId().raw(),
                        availableCreatures.plus(command.increaseBy()).raw()
                )
        );
    }

    @EventSourcingHandler
    void on(AvailableCreaturesChanged event) {
        this.availableCreatures = Amount.of(event.changedTo());
    }

    @CommandHandler
    void handle(RecruitCreature command) {
        new RecruitCreaturesNotExceedAvailableCreatures(availableCreatures, command.recruit()).verify();

        apply(
                new CreatureRecruited(
                        command.dwellingId().raw(),
                        command.creatureId().raw(),
                        command.recruit().raw(),
                        costPerTroop.multiply(command.recruit()).raw()
                )
        );
    }

    @EventSourcingHandler
    void on(CreatureRecruited event) {
        this.availableCreatures = this.availableCreatures.minus(Amount.of(event.recruited()));
    }

    Dwelling() {
        // required by Axon
    }
}
