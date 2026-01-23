package com.cts.telemetry.adapter.jersey;

import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.metrics.OptelMetrics;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Provider
public class OptelJerseyMetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_TIME_PROP = "optelStartTime";

    @Inject
    private OptelConfig config;

    @Context
    private ResourceInfo resourceInfo;

    // No-arg constructor for JAX-RS
    public OptelJerseyMetricsFilter() {
    }

    // Constructor for manual registration if needed
    public OptelJerseyMetricsFilter(OptelConfig config) {
        this.config = config;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (config != null && !config.getMetrics().isEnabled()) {
            return;
        }
        requestContext.setProperty(START_TIME_PROP, System.nanoTime());
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        if (config != null && !config.getMetrics().isEnabled()) {
            return;
        }
        Long startTime = (Long) requestContext.getProperty(START_TIME_PROP);
        if (startTime == null) {
            return;
        }

        double durationSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        recordMetrics(requestContext, responseContext, durationSeconds);
    }

    private void recordMetrics(ContainerRequestContext request, ContainerResponseContext response,
            double durationSeconds) {
        Map<String, String> attributes = new HashMap<>();

        attributes.put(com.cts.telemetry.api.HttpAttributes.METHOD.key(), request.getMethod());
        attributes.put(com.cts.telemetry.api.HttpAttributes.STATUS_CODE.key(), String.valueOf(response.getStatus()));
        if (config != null) {
            attributes.put(com.cts.telemetry.api.AppAttributes.SERVICE_NAME.key(), config.getServiceName());
        }

        String route = request.getUriInfo().getPath();
        attributes.put(com.cts.telemetry.api.HttpAttributes.ROUTE.key(), route);

        String tenantId = request.getHeaderString("X-Tenant-Id");
        if (tenantId != null) {
            attributes.put(com.cts.telemetry.api.AppAttributes.TENANT_ID.key(), tenantId);
        }

        if (resourceInfo != null && resourceInfo.getResourceClass() != null
                && resourceInfo.getResourceMethod() != null) {
            attributes.put(com.cts.telemetry.api.AppAttributes.API_NAME.key(),
                    resourceInfo.getResourceClass().getSimpleName() + "." + resourceInfo.getResourceMethod().getName());
        }

        // Error attributes
        // JAX-RS doesn't expose the exception directly in response filter easily unless
        // mapped.
        // We can check status code.
        if (response.getStatus() >= 500) {
            attributes.put(com.cts.telemetry.api.AppAttributes.ERROR_TYPE.key(), "HttpError");
            attributes.put(com.cts.telemetry.api.AppAttributes.ERROR_MESSAGE.key(),
                    response.getStatusInfo().getReasonPhrase());
        }

        attributes.put(com.cts.telemetry.api.HttpAttributes.SCHEME.key(),
                request.getUriInfo().getRequestUri().getScheme());

        // Protocol info not directly available in ContainerRequestContext in standard
        // JAX-RS easily without casting to implementation specific classes
        // or checking headers. We will try to get it if possible or skip.
        // For now, we can try to infer or leave empty if not critical, but spec says we
        // should have it.
        // Often "Via" header or similar might have it, or we can assume HTTP/1.1 if not
        // present, but better to be safe.
        // Let's check if we can get it from headers or properties.
        // Standard JAX-RS doesn't expose protocol version directly.

        attributes.put(com.cts.telemetry.api.HttpAttributes.SERVER_ADDRESS.key(),
                request.getUriInfo().getRequestUri().getHost());
        attributes.put(com.cts.telemetry.api.HttpAttributes.SERVER_PORT.key(),
                String.valueOf(request.getUriInfo().getRequestUri().getPort()));

        OptelMetrics.recordHttpDuration(durationSeconds, attributes);
    }
}
