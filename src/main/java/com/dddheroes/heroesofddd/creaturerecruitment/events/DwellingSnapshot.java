package com.dddheroes.heroesofddd.creaturerecruitment.events;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;

public class DwellingSnapshot {
    private final String dwellingId;
    private final String creatureId;
    private final Resources costPerTroop;
    private final int availableCreatures;

    public DwellingSnapshot(String dwellingId, String creatureId, Resources costPerTroop, int availableCreatures) {
        this.dwellingId = dwellingId;
        this.creatureId = creatureId;
        this.costPerTroop = costPerTroop;
        this.availableCreatures = availableCreatures;
    }

    public String getDwellingId() {
        return dwellingId;
    }

    public String getCreatureId() {
        return creatureId;
    }

    public Resources getCostPerTroop() {
        return costPerTroop;
    }

    public int getAvailableCreatures() {
        return availableCreatures;
    }
} 