package com.cts.telemetry.adapter.spring.metrics;

import com.cts.telemetry.api.DbAttributes;
import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.metrics.OptelMetrics;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for Database client metrics recording.
 */
public class DbMetricsHandler {

    private final OptelConfig config;

    public DbMetricsHandler(OptelConfig config) {
        this.config = config;
    }

    /**
     * Records database operation metrics.
     *
     * @param attributes Existing attributes from tracing or captured during
     *                   operation
     * @param duration   The duration in seconds (should match span duration)
     */
    public void recordMetrics(Map<String, String> attributes, double duration) {
        if (config.getMetrics() == null || !config.getMetrics().isEnabled()) {
            return;
        }

        Map<String, String> metricAttributes = new HashMap<>(attributes);

        // Ensure mandatory attributes are present if not already in the map
        // (Though they should be captured by the instrumentation)

        // Guard db.query.text emission by config flag if it exists in attributes
        if (!config.getTracing().getInstrument().getDb().isCaptureSql()) {
            metricAttributes.remove(DbAttributes.QUERY_TEXT.key());
        }

        // Record metrics
        OptelMetrics.recordDbOperationDuration(duration, metricAttributes);
    }
}
