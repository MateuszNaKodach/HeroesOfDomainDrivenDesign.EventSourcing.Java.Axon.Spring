package com.dddheroes.heroesofddd.resourcespool.application;

import com.dddheroes.heroesofddd.shared.domain.valueobjects.Resources;
import com.dddheroes.heroesofddd.shared.slices.write.Command;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ComposedCommandCostResolver implements CommandCostResolver {

    private final Map<Class<Command>, CommandCostResolver> resolvers;

    public ComposedCommandCostResolver(Set<CommandCostResolver> commandCostResolvers) {
        this.resolvers = commandCostResolvers
                .stream()
                .collect(Collectors.toMap(
                        CommandCostResolver::supportedCommandType,
                        Function.identity()
                ));
    }


    @Override
    public Resources resolve(Command command) {
        return resolvers.get(command.getClass()).resolve(command);
    }

    @Override
    public Class<Command> supportedCommandType() {
        return Command.class;
    }
}
