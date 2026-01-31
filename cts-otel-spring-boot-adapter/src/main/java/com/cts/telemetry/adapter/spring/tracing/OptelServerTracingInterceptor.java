package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.adapter.spring.api.strategy.HttpServerStrategy;
import com.cts.telemetry.api.strategy.TelemetryResult;
import com.cts.telemetry.adapter.spring.metrics.HttpMetricsHandler;
import com.cts.telemetry.config.OptelConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Unified HTTP interceptor that coordinates tracing and metrics for server
 * requests.
 * Uses separate handler classes for tracing and metrics to maintain clean
 * separation of concerns.
 *
 * This interceptor:
 * - Captures a single start time using System.nanoTime() to avoid timing
 * conflicts
 * - Delegates tracing logic to HttpTracingHandler
 * - Delegates metrics logic to HttpMetricsHandler
 * - Ensures both handlers work with the same timing data
 */
@Slf4j
public class OptelServerTracingInterceptor implements HandlerInterceptor {

    private final HttpTracingHandler tracingHandler;
    private final HttpMetricsHandler metricsHandler;

    private static final String SPAN_CONTEXT_ATTR = "optelSpanContext";
    private static final String START_TIME_ATTR = "optelStartTime";

    @Setter
    private HttpServerStrategy strategy;

    public OptelServerTracingInterceptor(OptelConfig config) {
        this.tracingHandler = new HttpTracingHandler(config);
        this.metricsHandler = new HttpMetricsHandler(config);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Always capture start time (single source of truth for both tracing and
        // metrics)
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        // Start span if tracing is enabled
        HttpTracingHandler.SpanContext spanContext = tracingHandler.startSpan(request, response);
        if (spanContext != null) {
            request.setAttribute(SPAN_CONTEXT_ATTR, spanContext);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception ex) {

        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        if (startTime == null) {
            return;
        }

        // Calculate duration once (shared by both tracing and metrics)
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        String route = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

        // Process application-level strategy
        TelemetryResult result = null;
        if (strategy != null) {
            result = strategy.process(request, response);
        }

        // Complete span and get error type
        HttpTracingHandler.SpanContext spanContext = (HttpTracingHandler.SpanContext) request
                .getAttribute(SPAN_CONTEXT_ATTR);
        String errorType = tracingHandler.completeSpan(spanContext, request, response, handler, ex, result);

        // Record metrics
        metricsHandler.recordMetrics(request, response, handler, ex, duration, route, errorType,
                result != null ? result.getAttributes() : null);
    }
}
