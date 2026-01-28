package com.cts.telemetry.adapter.spring.metrics.hikari;

import com.cts.telemetry.adapter.spring.metrics.DbConnectionPoolMetricsHandler;
import com.cts.telemetry.config.OptelConfig;
import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;

/**
 * Factory for creating OptelHikariMetricsTracker instances.
 */
public class OptelHikariMetricsTrackerFactory implements MetricsTrackerFactory {

    private final OptelConfig config;

    public OptelHikariMetricsTrackerFactory(OptelConfig config) {
        this.config = config;
    }

    @Override
    public IMetricsTracker create(String poolName, PoolStats poolStats) {
        DbConnectionPoolMetricsHandler handler = new DbConnectionPoolMetricsHandler(config, poolName);
        return new OptelHikariMetricsTracker(handler);
    }
}
