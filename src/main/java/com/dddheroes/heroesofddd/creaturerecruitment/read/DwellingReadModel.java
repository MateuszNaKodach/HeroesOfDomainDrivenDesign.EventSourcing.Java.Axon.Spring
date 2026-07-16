package com.dddheroes.heroesofddd.creaturerecruitment.read;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Map;
import java.util.Objects;

// Dual-mapped: the jakarta.persistence annotations exist ONLY so Hibernate ddl-auto creates and
// evolves the table (as it did before the reactive rewrite) — no JPA code ever touches this class.
// All runtime access goes through Spring Data R2DBC, which reads its own annotations exclusively.
@jakarta.persistence.Entity
@jakarta.persistence.Table(
        name = "read_model_dwelling",
        indexes = @jakarta.persistence.Index(name = "idx_read_model_dwelling_game_id", columnList = "gameId")
)
@Table("read_model_dwelling")
public class DwellingReadModel implements Persistable<String> {

    private String gameId;

    @jakarta.persistence.Id
    @Id
    private String dwellingId;

    private String creatureId;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @jakarta.persistence.Column(columnDefinition = "jsonb")
    private Map<String, Integer> costPerTroop;

    private Integer availableCreatures;

    // R2DBC has no merge/upsert semantics: save() on an entity with an assigned id issues an UPDATE
    // unless the entity says it is new. Instances built by the projector are new; instances
    // materialized from the database (no-arg constructor) are not.
    @jakarta.persistence.Transient
    @Transient
    private boolean isNew = false;

    public DwellingReadModel(
            String gameId,
            String dwellingId,
            String creatureId,
            Map<String, Integer> costPerTroop,
            Integer availableCreatures
    ) {
        this.gameId = gameId;
        this.dwellingId = dwellingId;
        this.creatureId = creatureId;
        this.costPerTroop = costPerTroop;
        this.availableCreatures = availableCreatures;
        this.isNew = true;
    }

    DwellingReadModel withAvailableCreatures(Integer availableCreatures) {
        this.availableCreatures = availableCreatures;
        return this;
    }

    DwellingReadModel withAvailableCreaturesDecreasedBy(Integer decreasedBy) {
        this.availableCreatures = this.availableCreatures - decreasedBy;
        return this;
    }

    public String getGameId() {
        return gameId;
    }

    public String getDwellingId() {
        return dwellingId;
    }

    public String getCreatureId() {
        return creatureId;
    }

    public Map<String, Integer> getCostPerTroop() {
        return costPerTroop;
    }

    public Integer getAvailableCreatures() {
        return availableCreatures;
    }

    @JsonIgnore
    @Override
    public String getId() {
        return dwellingId;
    }

    @JsonIgnore
    @Override
    public boolean isNew() {
        return isNew;
    }

    protected DwellingReadModel() {
        // Required by Spring Data for materializing database rows
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DwellingReadModel that = (DwellingReadModel) o;
        return Objects.equals(dwellingId, that.dwellingId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dwellingId);
    }
}
