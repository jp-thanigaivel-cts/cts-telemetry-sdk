package com.cts.telemetry.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.util.concurrent.Callable;

public class OptelTracer {

    private static final String TRACER_NAME = "cts.telemetry";

    public static Tracer getTracer() {
        return GlobalOpenTelemetry.getTracer(TRACER_NAME);
    }

    public static Span startSpan(String spanName, SpanKind kind) {
        return getTracer().spanBuilder(spanName)
                .setSpanKind(kind)
                .startSpan();
    }

    public static Span startSpan(String spanName, SpanKind kind, Context parent) {
        return getTracer().spanBuilder(spanName)
                .setParent(parent)
                .setSpanKind(kind)
                .startSpan();
    }

    public static <T> T traceCallable(String spanName, Callable<T> task) throws Exception {
        Span span = getTracer().spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            return task.call();
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    public static void traceRunnable(String spanName, Runnable task) {
        Span span = getTracer().spanBuilder(spanName)
                .setSpanKind(SpanKind.INTERNAL)
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            task.run();
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}
