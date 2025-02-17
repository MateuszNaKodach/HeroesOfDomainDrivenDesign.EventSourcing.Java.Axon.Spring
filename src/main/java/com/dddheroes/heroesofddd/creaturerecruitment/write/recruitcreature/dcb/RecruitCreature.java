package com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.dcb;

import com.dddheroes.heroesofddd.armies.write.addcreature.CanHaveMax7CreatureStacksInArmy;
import com.dddheroes.heroesofddd.armies.write.addcreature.CreatureAddedToArmy;
import com.dddheroes.heroesofddd.creaturerecruitment.write.DwellingId;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.CreatureRecruited;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.RecruitCreaturesNotExceedAvailableCreatures;
import com.dddheroes.heroesofddd.shared.Amount;
import com.dddheroes.heroesofddd.shared.ArmyId;
import com.dddheroes.heroesofddd.shared.CreatureId;
import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.eventhandling.gateway.EventGateway;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.messaging.MessageStream;
import org.axonframework.messaging.unitofwork.ProcessingContext;
import org.axonframework.modelling.repository.AsyncRepository;
import org.axonframework.modelling.repository.ManagedEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public record RecruitCreature(
        @TargetModelIdentifier // @TargetTag?
        TargetId targetId, // I don't like it compared to non-dcb, it's more technical
        CreatureId creatureId,
        Amount quantity
)  {
    record TargetId(DwellingId dwellingId, ArmyId armyId) {
    }
}

@Component
class RecruitCreatureCommandHandler {

    private final AsyncRepository<RecruitCreature.TargetId, State> stateRepository;
    private final EventGateway eventGateway; // EventSink!

    RecruitCreatureCommandHandler(
            AsyncRepository<RecruitCreature.TargetId, State> stateRepository,
            EventGateway eventGateway
    ) {
        this.stateRepository = stateRepository;
        this.eventGateway = eventGateway;
    }

    @CommandHandler
    CompletableFuture<?> handleOption1(
            RecruitCreature command,
            ProcessingContext processingContext
            //MetaData metaData, // optional
    ) {
        return stateRepository.load(command.targetId(), processingContext)
                .thenApply(ManagedEntity::entity)
                .thenApply(state -> decide(command, state))
                .thenAccept(eventGateway::publish);
    }

    private List<?> decide(RecruitCreature command, State state){
        new CanHaveMax7CreatureStacksInArmy(command.creatureId(), state.armyCreatures).verify();
        new RecruitCreaturesNotExceedAvailableCreatures(
                command.creatureId(),
                state.dwellingCreatures,
                command.creatureId(),
                command.quantity()
        ).verify();
        return List.of(
                CreatureRecruited.event( // the language its OK, for dwelling, but I don't like it in the Army context
                        command.targetId().dwellingId(),
                        command.creatureId(),
                        command.targetId().armyId(),
                        command.quantity()
                )
        );
    }

    // framework handle the rest from option1 and use repository under the hood
    @CommandHandler
    MessageStream handleOption2(
            RecruitCreature command,
            //MetaData metaData, // optional
            @Model State state
    ) {
        return decide(command, state);
    }

    // not sure about the name, other ideas:
    // @ConsistencyBoundary, @ModelState, @CommandModel, @State
    @Model
    record State(
            DwellingId dwellingId,
            ArmyId armyId,
            Amount dwellingCreatures,
            Map<CreatureId, Amount> armyCreatures
    ) {

        State() {
            this(null, null, Amount.zero(), Map.of());
        }

        @EventSourcingHandler
        State evolve(AvailableCreaturesChanged event) {
            return new State(
                    dwellingId,
                    armyId,
                    Amount.of(event.changedTo()),
                    Map.copyOf(armyCreatures)
            );
        }

        @EventSourcingHandler
        State evolve(CreatureAddedToArmy event) {
            return new State(
                    dwellingId,
                    new ArmyId(event.armyId()),
                    dwellingCreatures,
                    Map.copyOf(armyCreatures)
            );
        }
    }
}

@interface TargetModelIdentifier {
    // not sure about the name
}

@interface Model {
    // not sure about the name, other ideas:
    // @ConsistencyBoundary, @ModelState, @CommandModel
}

public @interface EventTag {

    String name() default ""; // by default use annotated property name
}

/*
class TestStatefulMessageHandler {

    interface StatefulMessageHandler {

    }

    @interface TargetModelIdentifier {

    }

    private final EventGateway eventGateway;

    TestStatefulMessageHandler(EventGateway eventGateway) {
        this.eventGateway = eventGateway;
    }

    AsyncRepository<String, CourseSubscriptionModel> courseSubscriptionRepo;

    // Annotation-based Axon impl
    public void handle(SubscribeStudentToCourseCommand command) {
        courseSubscriptionRepo.load(command.studentId(), StubProcessingContext.NONE)
                              .get()
                              .entity();
    }
    // Annotation-based Axon impl

    @CommandHandler
    public void handle(SubscribeStudentToCourseCommand command,
                       CourseSubscriptionModel student) {
        StudentSubscribedEvent studentEvent = student.doStuff(command);
    }

    record SubscribeStudentToCourseCommand(@TargetModelIdentifier String studentId,
                                           @TargetModelIdentifier String courseId) {
    }

    record CourseSubscriptionModel(String studentId) {

        StudentSubscribedEvent doStuff(SubscribeStudentToCourseCommand command) {
            return new StudentSubscribedEvent(studentId);
        }
    }

    record StudentSubscribedEvent(String studentId) {

    }
    record CourseEvent(String courseId) {

    }
}
 */