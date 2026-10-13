package com.dddheroes.compatprobes.commandinterop;

import com.dddheroes.compatprobes.commandinterop.ProbeCommands.EmptyStringProbe;
import com.dddheroes.compatprobes.commandinterop.ProbeCommands.FailProbe;
import com.dddheroes.compatprobes.commandinterop.ProbeCommands.ValueProbe;
import com.dddheroes.compatprobes.commandinterop.ProbeCommands.VoidProbe;
import org.axonframework.messaging.commandhandling.annotation.CommandHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Command handlers of the handler node. The dispatcher node does not create this bean, so Axon Server has to route
 * every probe command to a handler node.
 */
@Component
@ConditionalOnProperty(name = "probe.role", havingValue = "handler")
class ProbeCommandHandlers {

    @CommandHandler
    void handle(VoidProbe command) {
        // no result on purpose
    }

    @CommandHandler
    String handle(ValueProbe command) {
        return "pong";
    }

    @CommandHandler
    String handle(EmptyStringProbe command) {
        return "";
    }

    @CommandHandler
    String handle(FailProbe command) {
        throw new IllegalStateException("probe failure");
    }
}
