package com.cts.telemetry.adapter.spring.api.strategy;

import com.cts.telemetry.api.strategy.TelemetryResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Strategy for enriching HTTP Server telemetry data.
 */
@FunctionalInterface
public interface HttpServerStrategy {
    /**
     * Processes an HTTP request/response to determine the logical telemetry status
     * and attributes.
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @return The telemetry result
     */
    TelemetryResult process(HttpServletRequest request, HttpServletResponse response);
}
