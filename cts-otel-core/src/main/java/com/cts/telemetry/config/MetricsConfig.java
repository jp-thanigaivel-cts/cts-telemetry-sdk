package com.cts.telemetry.config;

import lombok.Data;

@Data
public class MetricsConfig {
    private boolean enabled = false;
    private MemoryMetricsConfig memory = new MemoryMetricsConfig();
}
