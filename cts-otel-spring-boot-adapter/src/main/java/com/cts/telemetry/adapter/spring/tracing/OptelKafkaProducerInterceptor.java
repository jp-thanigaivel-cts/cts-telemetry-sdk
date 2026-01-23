package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.api.SpanAttributes;
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
        @Override
        public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
            log.info("onSend is started");
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
            // Span is already ended in onSend for simplicity in this interceptor model,
            // ideally we would keep it open until ack, but interceptor API makes it hard to
            // pass context.
            // For this demo, we just trace the send initiation.
        }

        @Override
        public void close() {
        }

        @Override
        public void configure(Map<String, ?> configs) {
        }
    }

