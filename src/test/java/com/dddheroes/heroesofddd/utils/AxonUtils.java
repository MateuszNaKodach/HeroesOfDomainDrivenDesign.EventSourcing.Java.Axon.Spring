package com.dddheroes.heroesofddd.utils;

import com.dddheroes.heroesofddd.shared.GameId;
import com.dddheroes.heroesofddd.shared.PlayerId;
import org.axonframework.messaging.MetaData;

import static com.dddheroes.heroesofddd.shared.GameMetaData.*;

public class AxonUtils {

    public static MetaData aGameMetaData() {
        return with(GameId.random().raw(), PlayerId.random().raw());
    }
}
