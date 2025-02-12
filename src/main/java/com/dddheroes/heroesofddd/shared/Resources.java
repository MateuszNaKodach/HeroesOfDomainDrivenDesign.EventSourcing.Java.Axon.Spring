package com.dddheroes.heroesofddd.shared;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Resources {

    private final Map<ResourceType, Amount> raw;

    private Resources(Map<ResourceType, Amount> raw) {
        this.raw = raw;
    }

    public static Resources from(ResourceType type, Amount amount) {
        return new Resources(Map.of(type, amount));
    }

    public Resources plus(ResourceType type, Amount amount) {
        Map<ResourceType, Amount> newResources = new HashMap<>(this.raw);
        newResources.merge(type, amount, Amount::plus);
        return new Resources(newResources);
    }

    public Resources minus(ResourceType type, Amount amount) {
        Map<ResourceType, Amount> newResources = new HashMap<>(this.raw);
        newResources.merge(type, amount, Amount::minus);
        return new Resources(newResources);
    }

    public Resources plus(Map<ResourceType, Amount> additionalResources) {
        Map<ResourceType, Amount> newResources = new HashMap<>(this.raw);
        additionalResources.forEach((type, amount) ->
                                            newResources.merge(type, amount, Amount::plus)
        );
        return new Resources(newResources);
    }

    public Resources minus(Map<ResourceType, Amount> additionalResources) {
        Map<ResourceType, Amount> newResources = new HashMap<>(this.raw);
        additionalResources.forEach((type, amount) ->
                                            newResources.merge(type, amount, Amount::minus)
        );
        return new Resources(newResources);
    }

    public Resources multiply(int multiplier) {
        return new Resources(raw.entrySet().stream()
                                .collect(Collectors.toMap(
                                         Map.Entry::getKey,
                                         entry -> new Amount(entry.getValue().raw() * multiplier)
                                 )));
    }

    public Resources multiply(Amount amount) {
        return multiply(amount.raw());
    }

    public Amount amountOf(ResourceType type) {
        return raw.getOrDefault(type, Amount.zero());
    }

    public boolean contains(Resources resources){
        return resources.raw.entrySet().stream()
                .allMatch(entry -> raw.getOrDefault(entry.getKey(), Amount.zero()).raw() >= entry.getValue().raw());
    }

    public Map<String, Integer> raw() {
        return raw.entrySet().stream()
                  .collect(Collectors.toMap(
                                entry -> entry.getKey().name(),
                                entry -> entry.getValue().raw()
                        ));
    }

    public static Resources fromRaw(Map<String, Integer> raw) {
        Map<ResourceType, Amount> resources = raw.entrySet().stream()
                                                 .collect(Collectors.toMap(
                                                         entry -> ResourceType.from(entry.getKey()),
                                                         entry -> new Amount(entry.getValue())
                                                 ));
        return new Resources(resources);
    }

    // required by Jackson
    public Map<ResourceType, Amount> getRaw() {
        return new HashMap<>(raw);
    }
}