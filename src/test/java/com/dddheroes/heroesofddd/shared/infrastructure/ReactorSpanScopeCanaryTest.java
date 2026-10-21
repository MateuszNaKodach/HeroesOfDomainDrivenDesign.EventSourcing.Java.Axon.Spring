package com.dddheroes.heroesofddd.shared.infrastructure;

import com.dddheroes.heroesofddd.shared.infrastructure.ReactorSpanScopeWorkaroundConfiguration.BalancedSpanThreadLocalAccessor;
import io.micrometer.context.ThreadLocalAccessor;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.contextpropagation.ObservationAwareSpanThreadLocalAccessor;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/*
 * Canary for ReactorSpanScopeWorkaroundConfiguration: Micrometer's span accessor loses the outer span when Reactor
 * clears the span inside an already restored span scope.
 *
 * - balancedAccessor...: the workaround's behavior. Must always pass.
 * - micrometersAccessor... and axoniqFramework...: document that the libraries this app uses still need the
 *   workaround. When Micrometer Tracing (micrometer-metrics/tracing PR 1614) or Axoniq Framework
 *   (AxonIQ/axoniq-framework PR 329) ships the fix, the matching test fails. That is the signal to delete
 *   ReactorSpanScopeWorkaroundConfiguration and this test.
 *
 * Each scenario runs on its own short-lived thread, because the broken accessor leaves a scope open on the thread it
 * runs on.
 */
class ReactorSpanScopeCanaryTest {

    private final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(SpanExporter.composite()))
            .build();
    private final Tracer tracer = new OtelTracer(tracerProvider.get("canary"), new OtelCurrentTraceContext(), event -> {
    });

    @AfterEach
    void tearDown() {
        tracerProvider.close();
    }

    @Test
    void balancedAccessorRestoresTheOuterSpanAfterANestedClear() throws Exception {
        // given
        var accessor = new BalancedSpanThreadLocalAccessor(ObservationRegistry.create(), tracer);

        // when
        var outcome = runNestedClearOnFreshThread(accessor);

        // then
        assertThat(outcome).isEqualTo("outer span restored, thread clean");
    }

    @Test
    void micrometersAccessorStillLosesTheOuterSpanAfterANestedClear() throws Exception {
        // given
        var accessor = new ObservationAwareSpanThreadLocalAccessor(ObservationRegistry.create(), tracer);

        // when
        var outcome = runNestedClearOnFreshThread(accessor);

        // then the bug is still there: the assertion in restore(...) fails, or the outer span is gone
        assertThat(outcome).isNotEqualTo("outer span restored, thread clean");
    }

    @Test
    void axoniqFrameworkStillShipsWithoutItsBalancedAccessor() {
        // given the class that AxonIQ/axoniq-framework PR 329 adds
        var balancedAccessor = "io.axoniq.framework.tracing.micrometer.threadlocal.MicrometerSpanThreadLocalAccessor";

        // when / then
        assertThatThrownBy(() -> Class.forName(balancedAccessor)).isInstanceOf(ClassNotFoundException.class);
    }

    /**
     * Calls the accessor the way {@code ContextSnapshot} does for {@code snapshot(span) { snapshot(span) {
     * snapshot(without span) {} } }}: every set or clear is paired with one restore, innermost first.
     */
    private String runNestedClearOnFreshThread(ThreadLocalAccessor<Span> accessor) throws Exception {
        ExecutorService thread = Executors.newSingleThreadExecutor();
        try {
            return thread.submit(() -> {
                Span outer = tracer.nextSpan().name("outer").start();
                try {
                    Span beforeOuter = accessor.getValue();
                    accessor.setValue(outer);
                    Span beforeInner = accessor.getValue();
                    accessor.setValue(outer);
                    Span beforeClear = accessor.getValue();
                    accessor.setValue();
                    restore(accessor, beforeClear);
                    restore(accessor, beforeInner);
                    boolean outerRestored = outer.equals(tracer.currentSpan());
                    restore(accessor, beforeOuter);
                    boolean clean = tracer.currentSpan() == null;
                    return (outerRestored ? "outer span restored" : "outer span lost")
                            + (clean ? ", thread clean" : ", span leaked");
                } catch (AssertionError e) {
                    return "assertion failed: " + e.getMessage();
                } finally {
                    outer.end();
                }
            }).get(5, TimeUnit.SECONDS);
        } finally {
            thread.shutdownNow();
        }
    }

    private static void restore(ThreadLocalAccessor<Span> accessor, @Nullable Span previous) {
        if (previous == null) {
            accessor.restore();
        } else {
            accessor.restore(previous);
        }
    }
}
