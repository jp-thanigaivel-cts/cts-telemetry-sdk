package com.cts.telemetry.adapter.spring.metrics;

import com.cts.telemetry.api.AppAttributes;
import com.cts.telemetry.api.MessagingAttributes;
import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.metrics.OptelMetrics;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for Kafka Consumer metrics recording.
 */
public class KafkaConsumerMetricsHandler {

    private final OptelConfig config;

    public KafkaConsumerMetricsHandler(OptelConfig config) {
        this.config = config;
    }

    /**
     * Records consumer metrics for a receive operation.
     *
     * @param topic         The source topic
     * @param partition     The partition ID
     * @param consumerGroup The consumer group name
     * @param duration      The duration in seconds
     * @param errorType     The error type if failed (can be null)
     */
    public void recordReceiveMetrics(String topic, int partition, String consumerGroup,
            double duration, String errorType) {

        if (config.getMetrics() == null || !config.getMetrics().isEnabled()) {
            return;
        }

        Map<String, String> attributes = new HashMap<>();
        attributes.put(MessagingAttributes.SYSTEM.key(), "kafka");
        attributes.put(MessagingAttributes.OPERATION_NAME.key(), "receive");
        attributes.put(MessagingAttributes.OPERATION_TYPE.key(), "receive");
        attributes.put(MessagingAttributes.DESTINATION_NAME.key(), topic);
        attributes.put(MessagingAttributes.DESTINATION_PARTITION_ID.key(), String.valueOf(partition));
        attributes.put(MessagingAttributes.CONSUMER_GROUP_NAME.key(), consumerGroup);

        if (errorType != null) {
            attributes.put(MessagingAttributes.ERROR_TYPE.key(), errorType);
        }

        attributes.put(AppAttributes.SERVICE_NAME.key(), config.getServiceName());

        // Record metrics
        OptelMetrics.recordMessagingDuration(duration, attributes);
        OptelMetrics.recordMessageConsumed(1L, attributes);
    }

    /**
     * Records producer metrics for a process operation.
     *
     * @param topic         The source topic
     * @param partition     The partition ID
     * @param consumerGroup The consumer group name
     * @param duration      The duration in seconds
     * @param errorType     The error type if failed (can be null)
     */
    public void recordProcessMetrics(String topic, int partition, String consumerGroup,
            double duration, String errorType) {

        if (config.getMetrics() == null || !config.getMetrics().isEnabled()) {
            return;
        }

        Map<String, String> attributes = new HashMap<>();
        attributes.put(MessagingAttributes.SYSTEM.key(), "kafka");
        attributes.put(MessagingAttributes.OPERATION_NAME.key(), "process");
        attributes.put(MessagingAttributes.OPERATION_TYPE.key(), "process");
        attributes.put(MessagingAttributes.DESTINATION_NAME.key(), topic);
        attributes.put(MessagingAttributes.DESTINATION_PARTITION_ID.key(), String.valueOf(partition));
        attributes.put(MessagingAttributes.CONSUMER_GROUP_NAME.key(), consumerGroup);

        if (errorType != null) {
            attributes.put(MessagingAttributes.ERROR_TYPE.key(), errorType);
        }

        attributes.put(AppAttributes.SERVICE_NAME.key(), config.getServiceName());

        // Record metrics
        OptelMetrics.recordMessagingProcessDuration(duration, attributes);
    }
}
