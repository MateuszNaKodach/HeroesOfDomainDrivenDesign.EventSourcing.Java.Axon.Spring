package com.dddheroes.heroesofddd.resourcespool.events;

public sealed interface ResourcesPoolEvent permits ResourcesWithdrawn, ResourcesDeposited {

    String resourcesPoolId();
}
