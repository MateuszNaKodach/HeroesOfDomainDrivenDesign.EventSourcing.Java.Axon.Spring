package com.dddheroes.compatprobes.commandinterop;

/**
 * Commands exchanged between the handler node and the dispatcher node. Each one exercises a different kind of
 * command result. Every command carries a field, so that each converter serializes it as a non-empty JSON object.
 */
final class ProbeCommands {

    private ProbeCommands() {
    }

    /** Handled by a {@code void} command handler, so the result is {@code null}. */
    record VoidProbe(String id) {
    }

    /** Handled by a command handler returning {@code "pong"}. */
    record ValueProbe(String id) {
    }

    /** Handled by a command handler returning an empty {@code String}. */
    record EmptyStringProbe(String id) {
    }

    /** Handled by a command handler that throws {@code IllegalStateException("probe failure")}. */
    record FailProbe(String id) {
    }
}
