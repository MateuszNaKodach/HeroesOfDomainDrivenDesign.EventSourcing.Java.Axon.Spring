package com.dddheroes.heroesofddd.maintenance.write.transformeventrevisions;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Maintenance endpoint to fix Axon Framework 4 events stored without a payload revision (AxonIQ/AxonFramework#4625),
 * using the Axon Server Event Transformation feature.
 * <p>
 * Available only when {@code application.maintenance.enabled=true} and {@code axon.axonserver.enabled=true} (the
 * {@code axonserver} Spring profile) - it needs a connection to Axon Server.
 */
@ConditionalOnExpression("${application.maintenance.enabled:false} and ${axon.axonserver.enabled:false}")
@RestController
class EventRevisionTransformationRestApi {

    private final EventRevisionTransformation transformation;

    EventRevisionTransformationRestApi(EventRevisionTransformation transformation) {
        this.transformation = transformation;
    }

    /**
     * Replaces every event with an empty payload revision so it carries {@code revision}, and applies the
     * transformation.
     *
     * @param revision the revision to assign to events that currently have none (default {@code "0.0.1"})
     * @param compact  {@code true} compacts the store after applying to reclaim disk (default {@code false})
     */
    @CrossOrigin
    @PostMapping("/maintenance/event-store/transformations/fill-missing-revision")
    EventRevisionTransformation.Result fillMissingRevision(
            @RequestParam(defaultValue = "0.0.1") String revision,
            @RequestParam(defaultValue = "false") boolean compact) {
        return transformation.fillMissingRevision(revision, compact);
    }
}
