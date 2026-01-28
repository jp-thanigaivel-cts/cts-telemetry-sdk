package com.cts.telemetry.adapter.spring.metrics;

import com.cts.telemetry.api.DbAttributes;
import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.metrics.OptelMetrics;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for Database Connection Pool metrics recording.
 */
public class DbConnectionPoolMetricsHandler {

    private final OptelConfig config;
    private final String poolName;
    private final Map<String, String> baseAttributes;

    private static final String STATE_IDLE = "idle";
    private static final String STATE_USED = "used";

    public DbConnectionPoolMetricsHandler(OptelConfig config, String poolName) {
        this(config, poolName, null);
    }

    public DbConnectionPoolMetricsHandler(OptelConfig config, String poolName,
            Map<String, String> additionalAttributes) {
        this.config = config;
        this.poolName = poolName;
        this.baseAttributes = new HashMap<>();
        if (additionalAttributes != null) {
            this.baseAttributes.putAll(additionalAttributes);
        }
        this.baseAttributes.put(DbAttributes.CONNECTION_POOL_NAME.key(), poolName);
    }

    /**
     * Generates a unique pool name if none is provided.
     */
    public static String generatePoolName(String serverAddress, Integer serverPort, String dbNamespace) {
        StringBuilder sb = new StringBuilder();
        sb.append(serverAddress != null ? serverAddress : "unknown");
        if (serverPort != null) {
            sb.append(":").append(serverPort);
        }
        if (dbNamespace != null) {
            sb.append("/").append(dbNamespace);
        }
        return sb.toString();
    }

    public void incrementIdle() {
        record(1, STATE_IDLE);
    }

    public void decrementIdle() {
        record(-1, STATE_IDLE);
    }

    public void incrementUsed() {
        record(1, STATE_USED);
    }

    public void decrementUsed() {
        record(-1, STATE_USED);
    }

    private void record(long delta, String state) {
        if (config.getMetrics() == null || !config.getMetrics().isEnabled()) {
            return;
        }

        if (STATE_IDLE.equals(state)) {
            if (delta > 0)
                OptelMetrics.incrementIdle(poolName, baseAttributes);
            else
                OptelMetrics.decrementIdle(poolName, baseAttributes);
        } else {
            if (delta > 0)
                OptelMetrics.incrementUsed(poolName, baseAttributes);
            else
                OptelMetrics.decrementUsed(poolName, baseAttributes);
        }
    }
}
