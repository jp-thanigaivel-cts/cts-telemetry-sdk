package com.cts.telemetry.metrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;

import java.util.Map;

public class OptelMetrics {

    private static final String METER_NAME = "cts.telemetry";
    private static final String METRIC_NAME = "http.server.request.duration";

    private static DoubleHistogram httpDurationHistogram;

    private static Meter getMeter() {
        return GlobalOpenTelemetry.getMeter(METER_NAME);
    }

    private static synchronized DoubleHistogram getHttpDurationHistogram() {
        if (httpDurationHistogram == null) {
            httpDurationHistogram = getMeter()
                    .histogramBuilder(METRIC_NAME)
                    .setDescription("Duration of HTTP server requests")
                    .setUnit("ms")
                    .build();
        }
        return httpDurationHistogram;
    }

    public static void recordHttpDuration(double duration, Map<String, String> attributes) {
        AttributesBuilder attributesBuilder = Attributes.builder();
        if (attributes != null) {
            attributes.forEach(attributesBuilder::put);
        }
        getHttpDurationHistogram().record(duration, attributesBuilder.build());
    }
}
