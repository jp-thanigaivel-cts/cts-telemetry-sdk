package com.cts.telemetry.config;

import lombok.Data;

@Data
public class OptelConfig {
    private boolean enabled = true;
    private String serviceName = "unknown-service";
    private String endpoint = "http://localhost:4317";
    private ExporterType exporter = ExporterType.GRPC;
    private long exportIntervalSeconds = 10;

    // Tracing properties
    // Tracing properties
    private TracingConfig tracing = new TracingConfig();

    // Metrics properties
    private MetricsConfig metrics = new MetricsConfig();
}
