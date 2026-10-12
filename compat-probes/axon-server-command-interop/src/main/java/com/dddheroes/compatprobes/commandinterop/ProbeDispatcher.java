package com.dddheroes.compatprobes.commandinterop;

import com.dddheroes.compatprobes.commandinterop.ProbeCommands.EmptyStringProbe;
import com.dddheroes.compatprobes.commandinterop.ProbeCommands.FailProbe;
import com.dddheroes.compatprobes.commandinterop.ProbeCommands.ValueProbe;
import com.dddheroes.compatprobes.commandinterop.ProbeCommands.VoidProbe;
import org.axonframework.messaging.commandhandling.gateway.CommandGateway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Dispatches every probe command once through Axon Server and prints one machine-readable line per case:
 * <pre>
 * PROBE_RESULT|case|OK|value
 * PROBE_RESULT|case|ERROR|exception class: message
 * </pre>
 * A value is printed as {@code null} or as a double-quoted string. The only retried failure is a missing handler,
 * which covers the short window in which Axon Server has not yet seen the handler node's subscriptions.
 */
@Component
@ConditionalOnProperty(name = "probe.role", havingValue = "dispatcher")
class ProbeDispatcher implements ApplicationRunner {

    private static final int MAX_ATTEMPTS = 30;
    private static final long RETRY_DELAY_MILLIS = 500;
    private static final long RESULT_TIMEOUT_SECONDS = 15;
    private static final int MAX_MESSAGE_LENGTH = 240;

    private final CommandGateway commandGateway;

    ProbeDispatcher(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    @Override
    public void run(ApplicationArguments args) throws InterruptedException {
        probe("void", new VoidProbe(newId()), Void.class);
        probe("value", new ValueProbe(newId()), String.class);
        probe("emptyString", new EmptyStringProbe(newId()), String.class);
        probe("failure", new FailProbe(newId()), String.class);
    }

    private void probe(String caseName, Object command, Class<?> resultType) throws InterruptedException {
        for (int attempt = 1; ; attempt++) {
            try {
                Object result = commandGateway.send(command)
                                              .resultAs(resultType)
                                              .get(RESULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                print(caseName, "OK", formatValue(result));
                return;
            } catch (ExecutionException | CompletionException | java.util.concurrent.TimeoutException e) {
                Throwable failure = unwrap(e);
                if (isMissingHandler(failure) && attempt < MAX_ATTEMPTS) {
                    System.out.println("PROBE_RETRY|" + caseName + "|" + attempt + "|" + describe(failure));
                    Thread.sleep(RETRY_DELAY_MILLIS);
                    continue;
                }
                print(caseName, "ERROR", describe(failure));
                return;
            } catch (RuntimeException e) {
                print(caseName, "ERROR", describe(e));
                return;
            }
        }
    }

    private static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof ExecutionException || current instanceof CompletionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static boolean isMissingHandler(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getClass().getSimpleName().contains("NoHandler")
                    || String.valueOf(current.getMessage()).contains("No Handler")
                    || String.valueOf(current.getMessage()).contains("No handler")) {
                return true;
            }
        }
        return false;
    }

    private static String formatValue(Object value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static String describe(Throwable failure) {
        String message = String.valueOf(failure.getMessage()).replaceAll("\\s+", " ").replace('|', '/');
        if (message.length() > MAX_MESSAGE_LENGTH) {
            message = message.substring(0, MAX_MESSAGE_LENGTH) + "...";
        }
        return failure.getClass().getName() + ": " + message;
    }

    private static void print(String caseName, String outcome, String detail) {
        System.out.println("PROBE_RESULT|" + caseName + "|" + outcome + "|" + detail);
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
