package com.dddheroes.heroesofddd.resourcespool.application;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.slices.write.Command;

public interface CommandCostResolver {

    default Resources cost(Command command) {
        if (isSupported(command)) {
            return resolve(command);
        } else {
            return Resources.empty();
        }
    }

    Resources resolve(Command command);

    default boolean isSupported(Command command) {
        return supportedCommandType().isAssignableFrom(command.getClass());
    }

    Class<Command> supportedCommandType();
}
