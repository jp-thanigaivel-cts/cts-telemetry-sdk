package com.cts.telemetry.adapter.spring.metrics;

import com.cts.telemetry.api.AppAttributes;
import com.cts.telemetry.api.MessagingAttributes;
import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.metrics.OptelMetrics;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for Kafka Producer metrics recording.
 */
public class KafkaProducerMetricsHandler {

    private final OptelConfig config;

    public KafkaProducerMetricsHandler(OptelConfig config) {
        this.config = config;
    }

    /**
     * Records producer metrics for a send operation.
     *
     * @param topic         The destination topic
     * @param partition     The partition ID (can be null)
     * @param duration      The duration in seconds
     * @param errorType     The error type if failed (can be null)
     * @param serverAddress The bootstrap server address
     * @param serverPort    The bootstrap server port
     */
    public void recordMetrics(String topic, Integer partition, double duration,
            String errorType, String serverAddress, Integer serverPort) {

        if (config.getMetrics() == null || !config.getMetrics().isEnabled()) {
            return;
        }

        Map<String, String> attributes = new HashMap<>();
        attributes.put(MessagingAttributes.SYSTEM.key(), "kafka");
        attributes.put(MessagingAttributes.OPERATION_NAME.key(), "send");
        attributes.put(MessagingAttributes.OPERATION_TYPE.key(), "send");
        attributes.put(MessagingAttributes.DESTINATION_NAME.key(), topic);

        if (partition != null) {
            attributes.put(MessagingAttributes.DESTINATION_PARTITION_ID.key(), String.valueOf(partition));
        }

        if (errorType != null) {
            attributes.put(MessagingAttributes.ERROR_TYPE.key(), errorType);
        }

        if (serverAddress != null) {
            attributes.put(MessagingAttributes.SERVER_ADDRESS.key(), serverAddress);
        }

        if (serverPort != null) {
            attributes.put(MessagingAttributes.SERVER_PORT.key(), String.valueOf(serverPort));
        }

        attributes.put(AppAttributes.SERVICE_NAME.key(), config.getServiceName());

        // Record metrics
        OptelMetrics.recordMessagingDuration(duration, attributes);
        OptelMetrics.recordMessageSent(1L, attributes);
    }
}
