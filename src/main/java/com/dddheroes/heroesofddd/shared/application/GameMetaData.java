package com.dddheroes.heroesofddd.shared.application;

import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import org.axonframework.messaging.core.Metadata;

public class GameMetaData {

    public static final String GAME_ID_KEY = "gameId";
    public static final String PLAYER_ID_KEY = "playerId";

    public static Metadata with(String gameId) {
        return with(gameId, PlayerId.unknown().raw());
    }

    public static Metadata with(String gameId, String playerId) {
        return Metadata.with(GAME_ID_KEY, gameId)
                       .and(PLAYER_ID_KEY, playerId);
    }

    public static Metadata with(GameId gameId, PlayerId playerId) {
        return with(gameId.raw(), playerId.raw());
    }
}
