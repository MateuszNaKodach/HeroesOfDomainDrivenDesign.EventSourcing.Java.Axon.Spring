package com.dddheroes.heroesofddd.shared;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

// todo: think if rename to Resources (internal raw?)
public class Cost {

    private final Map<ResourceType, Amount> resources;

    private Cost(Map<ResourceType, Amount> resources) {
        this.resources = resources;
    }

    public static Cost resources(ResourceType type, Amount amount) {
        return new Cost(Map.of(type, amount));
    }

    public Cost plus(ResourceType type, Amount amount) {
        Map<ResourceType, Amount> newResources = new HashMap<>(this.resources);
        newResources.merge(type, amount, Amount::plus);
        return new Cost(newResources);
    }

    public Cost minus(ResourceType type, Amount amount) {
        Map<ResourceType, Amount> newResources = new HashMap<>(this.resources);
        newResources.merge(type, amount, Amount::minus);
        return new Cost(newResources);
    }

    public Cost plus(Map<ResourceType, Amount> additionalResources) {
        Map<ResourceType, Amount> newResources = new HashMap<>(this.resources);
        additionalResources.forEach((type, amount) ->
                                            newResources.merge(type, amount, Amount::plus)
        );
        return new Cost(newResources);
    }

    public Cost minus(Map<ResourceType, Amount> additionalResources) {
        Map<ResourceType, Amount> newResources = new HashMap<>(this.resources);
        additionalResources.forEach((type, amount) ->
                                            newResources.merge(type, amount, Amount::minus)
        );
        return new Cost(newResources);
    }

    public Cost multiply(int multiplier) {
        return new Cost(resources.entrySet().stream()
                                 .collect(Collectors.toMap(
                                         Map.Entry::getKey,
                                         entry -> new Amount(entry.getValue().raw() * multiplier)
                                 )));
    }

    public Cost multiply(Amount amount) {
        return multiply(amount.raw());
    }

    public Amount amountOf(ResourceType type) {
        return resources.getOrDefault(type, Amount.zero());
    }

    public Map<String, Integer> raw() {
        return resources.entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> entry.getKey().name(),
                                entry -> entry.getValue().raw()
                        ));
    }

    public static Cost fromRaw(Map<String, Integer> raw) {
        Map<ResourceType, Amount> resources = raw.entrySet().stream()
                                                 .collect(Collectors.toMap(
                                                         entry -> ResourceType.from(entry.getKey()),
                                                         entry -> new Amount(entry.getValue())
                                                 ));
        return new Cost(resources);
    }

    // required by Jackson
    public Map<ResourceType, Amount> getResources() {
        return new HashMap<>(resources);
    }
}