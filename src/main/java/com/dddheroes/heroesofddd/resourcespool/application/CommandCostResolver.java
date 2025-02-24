package com.dddheroes.heroesofddd.resourcespool.application;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.slices.write.Command;

public interface CommandCostResolver<C extends Command> {

    default Resources cost(C command) {
        if (isSupported(command)) {
            return resolve(command);
        } else {
            return Resources.empty();
        }
    }

    <T extends C> Resources resolve(T command);

    default boolean isSupported(C command) {
        return supportedCommandType().isAssignableFrom(command.getClass());
    }

    Class<? extends C> supportedCommandType();
}
