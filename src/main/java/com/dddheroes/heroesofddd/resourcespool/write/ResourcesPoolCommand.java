package com.dddheroes.heroesofddd.resourcespool.write;

import com.dddheroes.heroesofddd.shared.slices.write.Command;

public interface ResourcesPoolCommand extends Command {

    ResourcesPoolId resourcesPoolId();
}
