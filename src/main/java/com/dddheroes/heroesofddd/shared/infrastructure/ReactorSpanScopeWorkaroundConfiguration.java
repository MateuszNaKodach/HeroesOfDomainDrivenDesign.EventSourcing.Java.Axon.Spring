package com.dddheroes.heroesofddd.shared.infrastructure;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Workaround for a Micrometer Tracing bug that Axoniq Framework 5.4.0 still exposes: Reactor automatic context
 * propagation loses or leaks the Axon handler span on reactive (R2DBC) event handlers.
 * <p>
 * Micrometer's {@link ObservationAwareSpanThreadLocalAccessor}, which Axoniq Framework registers under
 * {@link ObservationAwareSpanThreadLocalAccessor#KEY}, overwrites the current scope when Reactor clears the span
 * (a subscriber whose Reactor Context has no span, e.g. inside the R2DBC driver) instead of opening a scope of its
 * own. Every following restore then closes the wrong scope: the outer span is lost, its {@code assert} in
 * {@code restore(...)} fails (fatal for the handler when assertions are enabled, as in tests), or the span stays
 * current on a worker thread. The fixes are micrometer-metrics/tracing PR 1614 and AxonIQ/axoniq-framework PR 329,
 * neither of them in a release this app uses yet.
 * <p>
 * Once the application is ready, this registers a balanced accessor under the same key, which replaces the
 * registered one: every set and every clear opens its own scope, and every restore closes exactly one.
 * {@code ReactorSpanScopeCanaryTest} fails once the registered accessor is balanced without this class, which is the
 * signal to delete it.
 * <p>
 * Set {@code application.workarounds.reactor-span-scope.enabled=false} to switch the workaround off.
 */
@Configuration
@ConditionalOnProperty(prefix = "application.workarounds.reactor-span-scope", name = "enabled",
        havingValue = "true", matchIfMissing = true)
class ReactorSpanScopeWorkaroundConfiguration {

    @Bean
    ApplicationListener<ApplicationReadyEvent> balancedSpanThreadLocalAccessorRegistration(
            ObjectProvider<Tracer> tracer,
            ObjectProvider<ObservationRegistry> observationRegistry
    ) {
        // without tracing (no Tracer bean) there is no span to propagate
        return event -> tracer.ifAvailable(t -> ContextRegistry.getInstance().registerThreadLocalAccessor(
                new BalancedSpanThreadLocalAccessor(observationRegistry.getIfAvailable(ObservationRegistry::create), t)
        ));
    }

    static final class BalancedSpanThreadLocalAccessor implements ThreadLocalAccessor<Span> {

        private final ObservationRegistry observationRegistry;
        private final Tracer tracer;
        private final ThreadLocal<Deque<Tracer.SpanInScope>> scopes = ThreadLocal.withInitial(ArrayDeque::new);

        BalancedSpanThreadLocalAccessor(ObservationRegistry observationRegistry, Tracer tracer) {
            this.observationRegistry = observationRegistry;
            this.tracer = tracer;
        }

        @Override
        public Object key() {
            return ObservationAwareSpanThreadLocalAccessor.KEY;
        }

        @Override
        public @Nullable Span getValue() {
            // same read as Micrometer's accessor: a span owned by the current Observation travels with the Observation
            Span current = tracer.currentSpan();
            Observation observation = observationRegistry.getCurrentObservation();
            if (observation == null || current == null) {
                return current;
            }
            TracingObservationHandler.TracingContext tracingContext =
                    observation.getContext().get(TracingObservationHandler.TracingContext.class);
            return tracingContext != null && current.equals(tracingContext.getSpan()) ? null : current;
        }

        @Override
        public void setValue(Span value) {
            scopes.get().push(tracer.withSpan(value));
        }

        @Override
        public void setValue() {
            // like Micrometer's accessor, never hide a span this accessor did not set (e.g. Spring's HTTP span)
            if (!scopes.get().isEmpty()) {
                scopes.get().push(tracer.withSpan(null));
            }
        }

        @Override
        public void restore(Span previousValue) {
            restore();
        }

        @Override
        public void restore() {
            Deque<Tracer.SpanInScope> stack = scopes.get();
            if (!stack.isEmpty()) {
                stack.pop().close();
            }
            if (stack.isEmpty()) {
                scopes.remove();
            }
        }
    }
}
