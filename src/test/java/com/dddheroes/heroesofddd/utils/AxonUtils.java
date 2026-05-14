package com.dddheroes.heroesofddd.utils;

import com.dddheroes.heroesofddd.shared.domain.identifiers.GameId;
import com.dddheroes.heroesofddd.shared.domain.identifiers.PlayerId;
import org.axonframework.messaging.core.Metadata;

import static com.dddheroes.heroesofddd.shared.application.GameMetaData.with;

public class AxonUtils {

    public static Metadata aGameMetaData() {
        return with(GameId.random().raw(), PlayerId.random().raw());
    }
}
