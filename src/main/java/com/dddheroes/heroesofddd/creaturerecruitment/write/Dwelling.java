package com.dddheroes.heroesofddd.creaturerecruitment.write;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.BuildDwelling;
import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.shared.Cost;
import com.dddheroes.heroesofddd.shared.CreatureId;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.util.HashMap;

import static org.axonframework.modelling.command.AggregateLifecycle.*;

@Aggregate
public class Dwelling {

    @AggregateIdentifier
    private DwellingId dwellingId;
    private CreatureId creatureId;
    private Cost costPerTroop;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    void handle(BuildDwelling command) {
        if (dwellingId != null) {
            throw new IllegalStateException("Dwelling already exists");
        }

        apply(
                new DwellingBuilt(
                        command.dwellingId().raw(),
                        command.creatureId().raw(),
                        new HashMap<>()
                )
        );
    }

    Dwelling() {
        // required by Axon
    }
}
