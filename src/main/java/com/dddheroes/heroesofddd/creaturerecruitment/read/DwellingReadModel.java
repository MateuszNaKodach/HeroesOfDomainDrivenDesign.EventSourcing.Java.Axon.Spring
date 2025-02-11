package com.dddheroes.heroesofddd.creaturerecruitment.read;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.Objects;

@Entity
@Table(
        schema = "creature_recruitment",
        name = "read_model_dwelling",
        indexes = @Index(name = "idx_game_id", columnList = "gameId")
)
public class DwellingReadModel {

    private String gameId;

    @Id
    private String dwellingId;

    private String creatureId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Integer> costPerTroop;

    private Integer availableCreatures;

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

    protected DwellingReadModel() {
        // Required by JPA
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
