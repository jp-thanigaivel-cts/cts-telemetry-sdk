package com.cts.telemetry.adapter.jersey;

import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.tracing.OptelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
@Provider
public class OptelJerseyTracingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String SCOPE_ATTR = "optelTraceScope";
    private static final String SPAN_ATTR = "optelTraceSpan";

    @Inject
    private OptelConfig config;

    @jakarta.ws.rs.core.Context
    private HttpServletRequest servletRequest;

    @jakarta.ws.rs.core.Context
    private ResourceInfo resourceInfo;

    public OptelJerseyTracingFilter() {
    }

    public OptelJerseyTracingFilter(OptelConfig config) {
        this.config = config;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (config != null
                && (!config.getTracing().isEnabled() || !config.getTracing().getInstrument().getHttp().isEnabled())) {
            return;
        }

        Context extractedContext = com.cts.telemetry.tracing.TraceUtils.extract(Context.current(), servletRequest,
                new HttpServletRequestGetter());

        String spanName = requestContext.getMethod() + " " + requestContext.getUriInfo().getPath();
        Span span = OptelTracer.startSpan(spanName, SpanKind.SERVER, extractedContext);

        Scope scope = span.makeCurrent();

        requestContext.setProperty(SCOPE_ATTR, scope);
        requestContext.setProperty(SPAN_ATTR, span);

        span.setAttribute(com.cts.telemetry.api.HttpAttributes.METHOD.key(), requestContext.getMethod());
        span.setAttribute(com.cts.telemetry.api.HttpAttributes.URL.key(),
                requestContext.getUriInfo().getRequestUri().toString());
        if (config != null) {
            span.setAttribute(com.cts.telemetry.api.AppAttributes.SERVICE_NAME.key(), config.getServiceName());
        }

        String tenantId = requestContext.getHeaderString("X-Tenant-Id");
        if (tenantId != null) {
            span.setAttribute(com.cts.telemetry.api.AppAttributes.TENANT_ID.key(), tenantId);
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Span span = (Span) requestContext.getProperty(SPAN_ATTR);
        Scope scope = (Scope) requestContext.getProperty(SCOPE_ATTR);

        if (span == null) {
            return;
        }

        try {
            span.setAttribute(com.cts.telemetry.api.HttpAttributes.STATUS_CODE.key(), responseContext.getStatus());

            // Propagate Trace ID to Response
            responseContext.getHeaders().add("traceId", span.getSpanContext().getTraceId());

            if (resourceInfo != null && resourceInfo.getResourceClass() != null
                    && resourceInfo.getResourceMethod() != null) {
                span.setAttribute(com.cts.telemetry.api.AppAttributes.API_NAME.key(),
                        resourceInfo.getResourceClass().getSimpleName() + "."
                                + resourceInfo.getResourceMethod().getName());
            }

            if (responseContext.getStatus() >= 500) {
                span.setStatus(StatusCode.ERROR, "Internal Server Error");
            }
        } finally {
            span.end();
            if (scope != null) {
                scope.close();
            }
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
