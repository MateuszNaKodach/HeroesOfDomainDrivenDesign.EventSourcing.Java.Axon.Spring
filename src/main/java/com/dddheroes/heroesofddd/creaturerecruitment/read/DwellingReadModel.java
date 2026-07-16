package com.dddheroes.heroesofddd.creaturerecruitment.read;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Map;
import java.util.Objects;

@Table("read_model_dwelling")
public class DwellingReadModel implements Persistable<String> {

    private String gameId;

    @Id
    private String dwellingId;

    private String creatureId;

    private Map<String, Integer> costPerTroop;

    private Integer availableCreatures;

    // R2DBC has no merge/upsert semantics: save() on an entity with an assigned id issues an UPDATE
    // unless the entity says it is new. Instances built by the projector are new; instances
    // materialized from the database (no-arg constructor) are not.
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
