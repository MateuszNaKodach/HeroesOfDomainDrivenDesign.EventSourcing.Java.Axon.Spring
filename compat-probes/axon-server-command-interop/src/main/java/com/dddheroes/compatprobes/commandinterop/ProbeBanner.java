package com.dddheroes.compatprobes.commandinterop;

import io.axoniq.framework.axonserver.connector.command.CommandConverter;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.security.CodeSource;

/**
 * Prints which variant and which framework jars this node runs with, and signals readiness of a handler node to the
 * matrix script through a {@code PROBE_READY} line.
 */
@Component
class ProbeBanner {

    private final String variant;
    private final String role;
    private final boolean workaroundEnabled;

    ProbeBanner(@Value("${probe.variant}") String variant,
                @Value("${probe.role}") String role,
                @Value("${probe.workaround.enabled}") boolean workaroundEnabled) {
        this.variant = variant;
        this.role = role;
        this.workaroundEnabled = workaroundEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    void printBanner() {
        System.out.println("PROBE_VARIANT|" + variant
                                   + "|role=" + role
                                   + "|workaround=" + workaroundEnabled
                                   + "|messaging=" + jarOf(CommandGateway.class)
                                   + "|connector=" + jarOf(CommandConverter.class));
        System.out.println("PROBE_READY|" + role);
    }

    private static String jarOf(Class<?> type) {
        CodeSource source = type.getProtectionDomain().getCodeSource();
        if (source == null || source.getLocation() == null) {
            return "unknown";
        }
        String location = source.getLocation().toString();
        if (location.endsWith("!/")) {
            location = location.substring(0, location.length() - 2);
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
