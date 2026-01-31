package com.cts.telemetry.adapter.spring.api.strategy;

import com.cts.telemetry.api.strategy.TelemetryResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Strategy for enriching Kafka Consumer telemetry data.
 */
@FunctionalInterface
public interface KafkaConsumerStrategy {
    /**
     * Processes a consumer record to determine the logical telemetry status and
     * attributes.
     *
     * @param record The consumer record being processed
     * @return The telemetry result
     */
    TelemetryResult process(ConsumerRecord<?, ?> record);
}
