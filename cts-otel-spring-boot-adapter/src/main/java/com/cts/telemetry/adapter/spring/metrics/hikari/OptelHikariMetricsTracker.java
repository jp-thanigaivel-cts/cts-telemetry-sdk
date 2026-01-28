package com.cts.telemetry.adapter.spring.metrics.hikari;

import com.cts.telemetry.adapter.spring.metrics.DbConnectionPoolMetricsHandler;
import com.zaxxer.hikari.metrics.IMetricsTracker;

/**
 * HikariCP IMetricsTracker implementation that delegates to
 * DbConnectionPoolMetricsHandler.
 */
public class OptelHikariMetricsTracker implements IMetricsTracker {

    private final DbConnectionPoolMetricsHandler handler;

    public OptelHikariMetricsTracker(DbConnectionPoolMetricsHandler handler) {
        this.handler = handler;
    }

    @Override
    public void recordConnectionCreatedMillis(long elapsedMillis) {
        handler.incrementIdle();
    }

    @Override
    public void recordConnectionAcquiredNanos(long elapsedNanos) {
        handler.decrementIdle();
        handler.incrementUsed();
    }

    @Override
    public void recordConnectionUsageMillis(long elapsedMillis) {
        handler.decrementUsed();
        handler.incrementIdle();
    }
}
