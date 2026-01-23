package com.cts.telemetry.config;

import lombok.Data;

@Data
public class MemoryMetricsConfig {
    private boolean enabled = false;
    private long pushIntervalMs = 5000;
}
