package com.cts.telemetry.metrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;

import java.util.Map;

public class OptelMetrics {

    private static final String METER_NAME = "cts.telemetry";
    private static final String HTTP_DURATION_METRIC = "http.server.request.duration";

    // Kafka Metrics
    private static final String MESSAGING_DURATION_METRIC = "messaging.client.operation.duration";
    private static final String MESSAGING_SENT_METRIC = "messaging.client.sent.messages";
    private static final String MESSAGING_CONSUMED_METRIC = "messaging.client.consumed.messages";
    private static final String MESSAGING_PROCESS_DURATION_METRIC = "messaging.process.duration";
    private static final String DB_DURATION_METRIC = "db.client.operation.duration";

    private static DoubleHistogram httpDurationHistogram;
    private static DoubleHistogram messagingDurationHistogram;
    private static DoubleHistogram messagingProcessDurationHistogram;
    private static DoubleHistogram dbDurationHistogram;
    private static LongCounter messagingSentCounter;
    private static LongCounter messagingConsumedCounter;

    private static Meter getMeter() {
        return GlobalOpenTelemetry.getMeter(METER_NAME);
    }

    private static synchronized DoubleHistogram getHttpDurationHistogram() {
        if (httpDurationHistogram == null) {
            httpDurationHistogram = getMeter()
                    .histogramBuilder(HTTP_DURATION_METRIC)
                    .setDescription("Duration of HTTP server requests")
                    .setUnit("ms")
                    .build();
        }
        return httpDurationHistogram;
    }

    private static synchronized DoubleHistogram getMessagingDurationHistogram() {
        if (messagingDurationHistogram == null) {
            messagingDurationHistogram = getMeter()
                    .histogramBuilder(MESSAGING_DURATION_METRIC)
                    .setDescription("Duration of messaging operations")
                    .setUnit("s")
                    .build();
        }
        return messagingDurationHistogram;
    }

    private static synchronized DoubleHistogram getMessagingProcessDurationHistogram() {
        if (messagingProcessDurationHistogram == null) {
            messagingProcessDurationHistogram = getMeter()
                    .histogramBuilder(MESSAGING_PROCESS_DURATION_METRIC)
                    .setDescription("Duration of messaging processing")
                    .setUnit("s")
                    .build();
        }
        return messagingProcessDurationHistogram;
    }

    private static synchronized LongCounter getMessagingSentCounter() {
        if (messagingSentCounter == null) {
            messagingSentCounter = getMeter()
                    .counterBuilder(MESSAGING_SENT_METRIC)
                    .setDescription("Number of messages sent")
                    .setUnit("1")
                    .build();
        }
        return messagingSentCounter;
    }

    private static synchronized LongCounter getMessagingConsumedCounter() {
        if (messagingConsumedCounter == null) {
            messagingConsumedCounter = getMeter()
                    .counterBuilder(MESSAGING_CONSUMED_METRIC)
                    .setDescription("Number of messages consumed")
                    .setUnit("1")
                    .build();
        }
        return messagingConsumedCounter;
    }

    private static synchronized DoubleHistogram getDbDurationHistogram() {
        if (dbDurationHistogram == null) {
            dbDurationHistogram = getMeter()
                    .histogramBuilder(DB_DURATION_METRIC)
                    .setDescription("Duration of database client operations")
                    .setUnit("s")
                    .build();
        }
        return dbDurationHistogram;
    }

    public static void recordHttpDuration(double duration, Map<String, String> attributes) {
        getHttpDurationHistogram().record(duration, buildAttributes(attributes));
    }

    public static void recordMessagingDuration(double durationInSeconds, Map<String, String> attributes) {
        getMessagingDurationHistogram().record(durationInSeconds, buildAttributes(attributes));
    }

    public static void recordMessagingProcessDuration(double durationInSeconds, Map<String, String> attributes) {
        getMessagingProcessDurationHistogram().record(durationInSeconds, buildAttributes(attributes));
    }

    public static void recordMessageSent(long count, Map<String, String> attributes) {
        getMessagingSentCounter().add(count, buildAttributes(attributes));
    }

    public static void recordMessageConsumed(long count, Map<String, String> attributes) {
        getMessagingConsumedCounter().add(count, buildAttributes(attributes));
    }

    public static void recordDbOperationDuration(double durationInSeconds, Map<String, String> attributes) {
        getDbDurationHistogram().record(durationInSeconds, buildAttributes(attributes));
    }

    private static Attributes buildAttributes(Map<String, String> attributes) {
        AttributesBuilder attributesBuilder = Attributes.builder();
        if (attributes != null) {
            attributes.forEach(attributesBuilder::put);
        }
        return attributesBuilder.build();
    }
}
