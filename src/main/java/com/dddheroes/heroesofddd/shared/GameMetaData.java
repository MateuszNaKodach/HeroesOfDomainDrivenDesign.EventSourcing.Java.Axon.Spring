package com.dddheroes.heroesofddd.shared;

import org.axonframework.messaging.MetaData;

public class GameMetaData {

    public static final String GAME_ID_KEY = "gameId";
    public static final String PLAYER_ID_KEY = "playerId";

    public static MetaData with(String gameId) {
        return with(gameId, PlayerId.unknown().raw());
    }

    public static MetaData with(String gameId, String playerId) {
        return MetaData.with(GAME_ID_KEY, gameId)
                       .and(PLAYER_ID_KEY, playerId);
    }

    public static MetaData with(GameId gameId, PlayerId playerId) {
        return with(gameId.raw(), playerId.raw());
    }
}
