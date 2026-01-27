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

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class OptelKafkaProducerInterceptor implements ProducerInterceptor<Object, Object> {

    private final KafkaProducerMetricsHandler metricsHandler;
    private final ThreadLocal<Long> startTimeThreadLocal = new ThreadLocal<>();

    public OptelKafkaProducerInterceptor() {
        this(OptelInitializer.getConfig() != null ? OptelInitializer.getConfig() : new OptelConfig());
    }

    public OptelKafkaProducerInterceptor(OptelConfig config) {
        this.metricsHandler = new KafkaProducerMetricsHandler(config);
    }

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        log.info("onSend is started");
        startTimeThreadLocal.set(System.currentTimeMillis());
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
        Long startTime = startTimeThreadLocal.get();
        if (startTime != null) {
            double duration = (System.currentTimeMillis() - startTime) / 1000.0;
            String errorType = (exception != null) ? exception.getClass().getSimpleName() : null;
            String topic = (metadata != null) ? metadata.topic() : null;
            Integer partition = (metadata != null) ? metadata.partition() : null;

            metricsHandler.recordMetrics(topic, partition, duration, errorType, null, null);
            startTimeThreadLocal.remove();
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }
}
