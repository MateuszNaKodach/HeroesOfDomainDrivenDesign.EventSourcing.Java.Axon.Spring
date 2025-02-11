package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(
        name = "read_model_built_dwelling",
        indexes = @Index(name = "idx_game_id", columnList = "gameId")
)
public class BuiltDwellingReadModel {

    private String gameId;

    @Id
    private String dwellingId;

    private String creatureId;

    public BuiltDwellingReadModel(String gameId, String dwellingId, String creatureId) {
        this.gameId = gameId;
        this.dwellingId = dwellingId;
        this.creatureId = creatureId;
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

    protected BuiltDwellingReadModel() {
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
        BuiltDwellingReadModel that = (BuiltDwellingReadModel) o;
        return Objects.equals(dwellingId, that.dwellingId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dwellingId);
    }
}
