package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.adapter.spring.metrics.KafkaProducerMetricsHandler;
import com.cts.telemetry.api.SpanAttributes;
import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.init.OptelInitializer;
import com.cts.telemetry.tracing.OptelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import org.apache.kafka.common.TopicPartition;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
public class OptelKafkaProducerInterceptor implements ProducerInterceptor<Object, Object> {

    private final KafkaProducerMetricsHandler metricsHandler;
    private final Map<TopicPartition, Queue<Long>> partitionStartTimes = new ConcurrentHashMap<>();
    private final Map<String, Queue<Long>> topicFallbackStartTimes = new ConcurrentHashMap<>();

    public OptelKafkaProducerInterceptor() {
        this(OptelInitializer.getConfig() != null ? OptelInitializer.getConfig() : new OptelConfig());
    }

    public OptelKafkaProducerInterceptor(OptelConfig config) {
        this.metricsHandler = new KafkaProducerMetricsHandler(config);
    }

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        log.info("onSend is started");

        long now = System.currentTimeMillis();
        Integer partition = record.partition();
        if (partition != null) {
            TopicPartition tp = new TopicPartition(record.topic(), partition);
            partitionStartTimes.computeIfAbsent(tp, k -> new ConcurrentLinkedQueue<>()).add(now);
        } else {
            topicFallbackStartTimes.computeIfAbsent(record.topic(), k -> new ConcurrentLinkedQueue<>()).add(now);
        }

        Span span = OptelTracer.startSpan("Kafka Send " + record.topic(), SpanKind.PRODUCER);

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_SYSTEM.key(), "kafka");
            span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_DESTINATION.key(), record.topic());
            span.setAttribute(SpanAttributes.MESSAGING_OPERATION_NAME.key(), "send");
            span.setAttribute(SpanAttributes.MESSAGING_OPERATION_TYPE.key(), "send");
            if (record.key() != null) {
                span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_KAFKA_MESSAGE_KEY.key(),
                        record.key().toString());
            }

            com.cts.telemetry.tracing.TraceUtils.inject(Context.current(), record.headers(),
                    (carrier, key, value) -> carrier.add(key, value.getBytes(StandardCharsets.UTF_8)));

            return record;
        } finally {
            span.end();
        }
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (metadata != null) {
            String topic = metadata.topic();
            int partition = metadata.partition();
            TopicPartition tp = new TopicPartition(topic, partition);

            Long startTime = null;
            Queue<Long> partitionQueue = partitionStartTimes.get(tp);
            if (partitionQueue != null) {
                startTime = partitionQueue.poll();
            }

            if (startTime == null) {
                Queue<Long> fallbackQueue = topicFallbackStartTimes.get(topic);
                if (fallbackQueue != null) {
                    startTime = fallbackQueue.poll();
                }
            }

            if (startTime != null) {
                double duration = (System.currentTimeMillis() - startTime) / 1000.0;
                String errorType = (exception != null) ? exception.getClass().getSimpleName() : null;

                metricsHandler.recordMetrics(topic, partition, duration, errorType, null, null);
            }
        }
    }

    @Override
    public void close() {
        partitionStartTimes.clear();
        topicFallbackStartTimes.clear();
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
