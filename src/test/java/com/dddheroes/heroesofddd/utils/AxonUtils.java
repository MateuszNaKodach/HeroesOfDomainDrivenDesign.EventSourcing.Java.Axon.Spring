package com.dddheroes.heroesofddd.utils;

import org.axonframework.messaging.MetaData;

import java.util.UUID;

import static com.dddheroes.heroesofddd.shared.GameMetaData.*;

public class AxonUtils {

    public static MetaData aGameMetaData() {
        return withId(UUID.randomUUID().toString());
    }
}
