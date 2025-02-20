package com.dddheroes.heroesofddd.shared.domain.valueobjects;

import java.util.Arrays;

public enum ResourceType {
    GOLD,
    WOOD,
    ORE,
    MERCURY,
    SULFUR,
    CRYSTAL,
    GEMS;

    public static ResourceType from(String name) {
        return Arrays.stream(values())
                     .filter(type -> type.name().equalsIgnoreCase(name))
                     .findFirst()
                     .orElseThrow(() -> new IllegalArgumentException("No enum constant " + ResourceType.class.getCanonicalName() + "." + name));
    }
}
