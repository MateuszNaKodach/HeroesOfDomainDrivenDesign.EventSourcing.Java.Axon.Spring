package com.dddheroes.heroesofddd.astrologers.automation.whenweeksymbolproclaimedthenincreasedwellingavailablecreatures;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

// Dual-mapped: the jakarta.persistence annotations exist ONLY so Hibernate ddl-auto creates and
// evolves the table (as it did before the reactive rewrite) — no JPA code ever touches this class.
// All runtime access goes through Spring Data R2DBC, which reads its own annotations exclusively.
@jakarta.persistence.Entity
@jakarta.persistence.Table(
        name = "read_model_built_dwelling",
        indexes = @jakarta.persistence.Index(name = "idx_read_model_built_dwelling_game_id", columnList = "gameId")
)
@Table("read_model_built_dwelling")
public class BuiltDwellingReadModel implements Persistable<String> {

    private String gameId;

    @jakarta.persistence.Id
    @Id
    private String dwellingId;

    private String creatureId;

    // R2DBC has no merge/upsert semantics: save() on an entity with an assigned id issues an UPDATE
    // unless the entity says it is new. Instances built by the automation are new; instances
    // materialized from the database (no-arg constructor) are not.
    @jakarta.persistence.Transient
    @Transient
    private boolean isNew = false;

    public BuiltDwellingReadModel(String gameId, String dwellingId, String creatureId) {
        this.gameId = gameId;
        this.dwellingId = dwellingId;
        this.creatureId = creatureId;
        this.isNew = true;
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

    @Override
    public String getId() {
        return dwellingId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    protected BuiltDwellingReadModel() {
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
        BuiltDwellingReadModel that = (BuiltDwellingReadModel) o;
        return Objects.equals(dwellingId, that.dwellingId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dwellingId);
    }
}
