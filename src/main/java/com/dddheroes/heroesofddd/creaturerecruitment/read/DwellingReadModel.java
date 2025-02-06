package com.dddheroes.heroesofddd.creaturerecruitment.read;

import com.dddheroes.heroesofddd.creaturerecruitment.write.builddwelling.DwellingBuilt;
import com.dddheroes.heroesofddd.creaturerecruitment.write.changeavailablecreatures.AvailableCreaturesChanged;
import com.dddheroes.heroesofddd.creaturerecruitment.write.recruitcreature.CreatureRecruited;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.axonframework.eventhandling.EventHandler;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

public class DwellingReadModel {

    @org.springframework.stereotype.Repository
    public interface Repository extends JpaRepository<State, String> {

    }

    @Entity
    @Table(name = "read_model_dwelling")
    public static class State {

        @Id
        private String id;

        private String creatureId;

        @JdbcTypeCode(SqlTypes.JSON)
        @Column(columnDefinition = "jsonb")
        private Map<String, Integer> costPerTroop;

        private Integer availableCreatures;

        public State(String id, String creatureId, Map<String, Integer> costPerTroop, Integer availableCreatures) {
            this.id = id;
            this.creatureId = creatureId;
            this.costPerTroop = costPerTroop;
            this.availableCreatures = availableCreatures;
        }

        protected State() {
            // Required by JPA
        }

        public State withAvailableCreatures(Integer availableCreatures) {
            this.availableCreatures = availableCreatures;
            return this;
        }

        public State withAvailableCreaturesDecreasedBy(Integer decreasedBy) {
            this.availableCreatures = this.availableCreatures - decreasedBy;
            return this;
        }
    }

    @Component
    public static class Projection {

        private final DwellingReadModel.Repository repository;

        public Projection(DwellingReadModel.Repository repository) {
            this.repository = repository;
        }

        @EventHandler
        void on(DwellingBuilt event) {
            var state = new State(
                    event.dwellingId(),
                    event.creatureId(),
                    event.costPerTroop(),
                    0
            );
            repository.save(state);
        }

        @EventHandler
        void on(AvailableCreaturesChanged event) {
            repository.findById(event.dwellingId())
                      .map(state -> state.withAvailableCreatures(event.changedTo()))
                      .ifPresent(repository::save);
        }

        @EventHandler
        void on(CreatureRecruited event) {
            repository.findById(event.dwellingId())
                      .map(state -> state.withAvailableCreaturesDecreasedBy(event.quantity()))
                      .ifPresent(repository::save);
        }
    }
}
