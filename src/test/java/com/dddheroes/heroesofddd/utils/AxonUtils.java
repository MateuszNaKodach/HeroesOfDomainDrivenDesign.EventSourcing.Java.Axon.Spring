package com.dddheroes.heroesofddd.utils;

import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import org.axonframework.messaging.MetaData;

import static com.dddheroes.heroesofddd.shared.application.GameMetaData.*;

public class AxonUtils {

    public static MetaData aGameMetaData() {
        return with(GameId.random().raw(), PlayerId.random().raw());
    }
}
