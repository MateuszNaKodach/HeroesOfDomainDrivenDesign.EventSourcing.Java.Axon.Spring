package com.dddheroes.heroesofddd.resourcespool.application;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import org.axonframework.messaging.commandhandling.CommandMessage;
import org.axonframework.messaging.core.QualifiedName;
import org.axonframework.messaging.core.unitofwork.ProcessingContext;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ComposedCommandCostResolver implements CommandCostResolver {

    private final Map<QualifiedName, CommandCostResolver> resolvers;

    public ComposedCommandCostResolver(Set<CommandCostResolver> commandCostResolvers) {
        this.resolvers = commandCostResolvers
                .stream()
                .collect(Collectors.toMap(
                        CommandCostResolver::supportedCommand,
                        Function.identity()
                ));
    }

    @Override
    public Resources resolve(CommandMessage message, ProcessingContext context) {
        var resolver = resolvers.get(message.type().qualifiedName());
        if (resolver == null) {
            return Resources.empty();
        }
        return resolver.resolve(message, context);
    }

    @Override
    public boolean isSupported(CommandMessage message) {
        return resolvers.containsKey(message.type().qualifiedName());
    }

    @Override
    public QualifiedName supportedCommand() {
        throw new UnsupportedOperationException("ComposedCommandCostResolver delegates to specific resolvers");
    }
}
