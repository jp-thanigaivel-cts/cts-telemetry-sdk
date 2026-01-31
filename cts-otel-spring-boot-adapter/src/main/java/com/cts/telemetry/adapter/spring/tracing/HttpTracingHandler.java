package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.api.AppAttributes;
import com.cts.telemetry.api.HttpAttributes;
import com.cts.telemetry.api.strategy.StatusType;
import com.cts.telemetry.api.strategy.TelemetryResult;
import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.tracing.OptelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Handler for HTTP tracing (span creation and management).
 * Encapsulates all tracing-related logic for HTTP server requests.
 */
public class HttpTracingHandler {

    private final OptelConfig config;

    public HttpTracingHandler(OptelConfig config) {
        this.config = config;
    }

    /**
     * Starts a span for the incoming HTTP request.
     * 
     * @return SpanContext containing the span and scope, or null if tracing is
     *         disabled
     */
    public SpanContext startSpan(HttpServletRequest request, HttpServletResponse response) {
        if (config.getTracing() == null || !config.getTracing().isEnabled()
                || !config.getTracing().getInstrument().getHttp().isEnabled()) {
            return null;
        }

        Context extractedContext = com.cts.telemetry.tracing.TraceUtils.extract(
                Context.current(), request, new HttpServletRequestGetter());

        String spanName = request.getMethod() + " " + request.getRequestURI();
        Span span = OptelTracer.startSpan(spanName, SpanKind.SERVER, extractedContext);
        Scope scope = span.makeCurrent();

        // Set initial span attributes
        span.setAttribute(HttpAttributes.METHOD.key(), request.getMethod());
        span.setAttribute(HttpAttributes.URL.key(), request.getRequestURL().toString());
        span.setAttribute(AppAttributes.SERVICE_NAME.key(), config.getServiceName());

        // Propagate Trace ID to Response
        response.setHeader("traceId", span.getSpanContext().getTraceId());

        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId != null) {
            span.setAttribute(AppAttributes.TENANT_ID.key(), tenantId);
        }

        return new SpanContext(span, scope);
    }

    /**
     * Completes the span with final attributes and status.
     * 
     * @return error type if an error occurred, null otherwise
     */
    public String completeSpan(SpanContext spanContext, HttpServletRequest request,
            HttpServletResponse response, Object handler, Exception ex, TelemetryResult result) {
        if (spanContext == null) {
            return null;
        }

        Span span = spanContext.getSpan();
        String errorType = null;

        try {
            span.setAttribute(HttpAttributes.STATUS_CODE.key(), response.getStatus());

            String route = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (route != null) {
                span.updateName(request.getMethod() + " " + route);
                span.setAttribute(HttpAttributes.ROUTE.key(), route);
            }

            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                span.setAttribute(AppAttributes.API_NAME.key(),
                        handlerMethod.getBeanType().getSimpleName() + "." + handlerMethod.getMethod().getName());
            }

            if (ex != null) {
                span.recordException(ex);
                span.setStatus(StatusCode.ERROR, ex.getMessage());
                errorType = ex.getClass().getName();
            } else if (result != null && result.getStatusType() == StatusType.ERROR) {
                span.setStatus(StatusCode.ERROR, result.getStatusDescription() != null
                        ? result.getStatusDescription()
                        : "Logical error signaled by application");
                errorType = result.getStatusCode() != null ? result.getStatusCode() : "AppError";
            } else if (response.getStatus() >= 500) {
                span.setStatus(StatusCode.ERROR, "Internal Server Error");
                errorType = "500";
            }

            if (result != null) {
                if (result.getAttributes() != null) {
                    result.getAttributes().forEach(span::setAttribute);
                }
                if (result.getStatusCode() != null) {
                    span.setAttribute("app.status_code", result.getStatusCode());
                }
            }

        } finally {
            span.end();
            if (spanContext.getScope() != null) {
                spanContext.getScope().close();
            }
        }

        return errorType;
    }

    /**
     * Container for span and scope.
     */
    public static class SpanContext {
        private final Span span;
        private final Scope scope;

        public SpanContext(Span span, Scope scope) {
            this.span = span;
            this.scope = scope;
        }

        public Span getSpan() {
            return span;
        }

        public Scope getScope() {
            return scope;
        }
    }

    private static class HttpServletRequestGetter
            implements io.opentelemetry.context.propagation.TextMapGetter<HttpServletRequest> {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
            return () -> new EnumerationIterator<>(carrier.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
            return carrier.getHeader(key);
        }
    }

    private static class EnumerationIterator<E> implements java.util.Iterator<E> {
        private final java.util.Enumeration<E> enumeration;

        public EnumerationIterator(java.util.Enumeration<E> enumeration) {
            this.enumeration = enumeration;
        }

        @Override
        public boolean hasNext() {
            return enumeration.hasMoreElements();
        }

        @Override
        public E next() {
            return enumeration.nextElement();
        }
    }
}
