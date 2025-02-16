package com.dddheroes.heroesofddd.maintenance.write.resetprocessor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@ConditionalOnProperty(name = "application.maintenance.enabled", havingValue = "true")
@RestController
class ResetStreamProcessorRestApi {

    private final StreamProcessorsOperations streamProcessorsOperations;

    ResetStreamProcessorRestApi(StreamProcessorsOperations streamProcessorsOperations) {
        this.streamProcessorsOperations = streamProcessorsOperations;
    }

    @CrossOrigin
    @PostMapping("/maintenance/processors/{name}/resets")
    void resetProcessor(@PathVariable String name) {
        streamProcessorsOperations.reset(name);
    }
}
