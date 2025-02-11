package com.dddheroes.heroesofddd.shared;

import org.axonframework.messaging.MetaData;

public class GameMetaData {

    public static final String KEY = "gameId";

    public static MetaData withId(String gameId) {
        return MetaData.with(KEY, gameId);
    }

    public static MetaData withId(GameId gameId) {
        return withId(gameId.raw());
    }
}
