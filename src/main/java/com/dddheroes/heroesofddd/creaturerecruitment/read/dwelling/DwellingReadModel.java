package com.dddheroes.heroesofddd.creaturerecruitment.read.dwelling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

@Entity
@Table(name = "read_model_dwelling")
class DwellingReadModel {

    @Id
    private String id;

    private String creatureId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Integer> costPerTroop;

    private Integer availableCreatures;

    DwellingReadModel(String id, String creatureId, Map<String, Integer> costPerTroop, Integer availableCreatures) {
        this.id = id;
        this.creatureId = creatureId;
        this.costPerTroop = costPerTroop;
        this.availableCreatures = availableCreatures;
    }

    protected DwellingReadModel() {
        // Required by JPA
    }

    DwellingReadModel withAvailableCreatures(Integer availableCreatures) {
        this.availableCreatures = availableCreatures;
        return this;
    }

    DwellingReadModel withAvailableCreaturesDecreasedBy(Integer decreasedBy) {
        this.availableCreatures = this.availableCreatures - decreasedBy;
        return this;
    }
}
