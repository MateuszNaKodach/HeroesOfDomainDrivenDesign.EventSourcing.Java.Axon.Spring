package com.dddheroes.heroesofddd.resourcespool.application;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;

public interface CommandCostResolver {

    default Resources cost(CommandMessage message, ProcessingContext context) {
        if (isSupported(message)) {
            return resolve(message, context);
        }
        return Resources.empty();
    }

    Resources resolve(CommandMessage message, ProcessingContext context);

    default boolean isSupported(CommandMessage message) {
        return supportedCommand().equals(message.type().qualifiedName());
    }

    QualifiedName supportedCommand();
}
