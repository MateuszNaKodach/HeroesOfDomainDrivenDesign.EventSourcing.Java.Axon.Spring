package com.dddheroes.compatprobes.commandinterop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Entry point of the probe. The {@code probe.role} property decides whether this node handles the probe commands
 * ({@code handler}, keeps running until stopped) or dispatches them once through Axon Server and exits
 * ({@code dispatcher}).
 */
@SpringBootApplication
public class CommandInteropProbeApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(CommandInteropProbeApplication.class, args);
        if ("dispatcher".equals(context.getEnvironment().getProperty("probe.role"))) {
            System.exit(SpringApplication.exit(context));
        }
    }
}
