package com.cts.telemetry.adapter.spring.metrics;

import com.cts.telemetry.api.AppAttributes;
import com.cts.telemetry.api.HttpAttributes;
import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.metrics.OptelMetrics;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for HTTP metrics recording.
 * Encapsulates all metrics-related logic for HTTP server requests.
 */
public class HttpMetricsHandler {

    private final OptelConfig config;

    public HttpMetricsHandler(OptelConfig config) {
        this.config = config;
    }

    /**
     * Records HTTP metrics for the completed request.
     * 
     * @param duration  the request duration in milliseconds
     * @param route     the matched route pattern (may be null)
     * @param errorType the error type if an error occurred (may be null)
     */
    public void recordMetrics(HttpServletRequest request, HttpServletResponse response,
            Object handler, Exception ex, double duration,
            String route, String errorType, Map<String, String> additionalAttributes) {

        if (config.getMetrics() == null || !config.getMetrics().isEnabled()) {
            return;
        }

        Map<String, String> attributes = new HashMap<>();

        // HTTP attributes
        attributes.put(HttpAttributes.METHOD.key(), request.getMethod());
        attributes.put(HttpAttributes.SCHEME.key(), request.getScheme());
        attributes.put(HttpAttributes.STATUS_CODE.key(), String.valueOf(response.getStatus()));

        if (route != null) {
            attributes.put(HttpAttributes.ROUTE.key(), route);
        } else {
            // Fallback to URI if route not available
            String uri = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (uri != null) {
                attributes.put(HttpAttributes.ROUTE.key(), uri);
            }
        }

        // Error attributes
        if (errorType != null) {
            attributes.put(HttpAttributes.ERROR_TYPE.key(), errorType);
        } else if (ex != null) {
            attributes.put(HttpAttributes.ERROR_TYPE.key(), ex.getClass().getSimpleName());
        }

        // Protocol attributes
        String protocol = request.getProtocol(); // e.g. HTTP/1.1
        if (protocol != null) {
            String[] parts = protocol.split("/");
            if (parts.length == 2) {
                attributes.put(HttpAttributes.NETWORK_PROTOCOL_NAME.key(), parts[0]);
                attributes.put(HttpAttributes.NETWORK_PROTOCOL_VERSION.key(), parts[1]);
            } else {
                attributes.put(HttpAttributes.NETWORK_PROTOCOL_NAME.key(), protocol);
            }
        }

        // Server attributes
        attributes.put(HttpAttributes.SERVER_ADDRESS.key(), request.getServerName());
        attributes.put(HttpAttributes.SERVER_PORT.key(), String.valueOf(request.getServerPort()));

        // Application attributes
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId != null) {
            attributes.put(AppAttributes.TENANT_ID.key(), tenantId);
        }

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            attributes.put(AppAttributes.API_NAME.key(),
                    handlerMethod.getBeanType().getSimpleName() + "." + handlerMethod.getMethod().getName());
        }

        if (additionalAttributes != null) {
            attributes.putAll(additionalAttributes);
        }

        attributes.put(AppAttributes.SERVICE_NAME.key(), config.getServiceName());

        // Record the duration metric
        OptelMetrics.recordHttpDuration(duration, attributes);
    }
}
