package com.dddheroes.heroesofddd.resourcespool.application;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.slices.write.Command;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ComposedCommandCostResolver implements CommandCostResolver<Command> {

    private final Map<Class<? extends Command>, CommandCostResolver<?>> resolvers;

    public ComposedCommandCostResolver(Set<CommandCostResolver<?>> commandCostResolvers) {
        this.resolvers = commandCostResolvers
                .stream()
                .collect(Collectors.toMap(
                        CommandCostResolver::supportedCommandType,
                        Function.identity()
                ));
    }


    @Override
    public <T extends Command> Resources resolve(T command) {
        @SuppressWarnings("unchecked")
        var resolver = (CommandCostResolver<T>) resolvers.get(command.getClass());
        if (resolver == null) {
            return Resources.empty();
        }
        return resolver.resolve(command);
    }

    @Override
    public Class<? extends Command> supportedCommandType() {
        return Command.class;
    }
}
