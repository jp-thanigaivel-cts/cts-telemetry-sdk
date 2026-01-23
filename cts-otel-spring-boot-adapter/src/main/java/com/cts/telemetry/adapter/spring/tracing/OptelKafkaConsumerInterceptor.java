package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.tracing.OptelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class OptelKafkaConsumerInterceptor implements ConsumerInterceptor<Object, Object> {

    @Override
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        records.forEach(record -> {
            log.info("onConsume is started");

            Context extractedContext = com.cts.telemetry.tracing.TraceUtils.extract(
                    Context.current(),
                    record.headers(),
                    new HeadersGetter());

            Span span = OptelTracer.startSpan("Kafka Consume " + record.topic(), SpanKind.CONSUMER, extractedContext);

            span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_SYSTEM.key(), "kafka");
            span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_DESTINATION.key(), record.topic());
            span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_KAFKA_PARTITION.key(), record.partition());
            span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_KAFKA_OFFSET.key(), record.offset());

            span.makeCurrent();
        });
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
    }

    @Override
    public void close() {
    }

    @Override
    public void configure(Map<String, ?> configs) {
    }

    private static class HeadersGetter
            implements io.opentelemetry.context.propagation.TextMapGetter<org.apache.kafka.common.header.Headers> {
        @Override
        public Iterable<String> keys(org.apache.kafka.common.header.Headers carrier) {
            return () -> new HeadersIterator(carrier.iterator());
        }

        @Override
        public String get(org.apache.kafka.common.header.Headers carrier, String key) {
            org.apache.kafka.common.header.Header header = carrier.lastHeader(key);
            if (header != null) {
                return new String(header.value(), StandardCharsets.UTF_8);
            }
            return null;
        }
    }

    private static class HeadersIterator implements java.util.Iterator<String> {
        private final java.util.Iterator<org.apache.kafka.common.header.Header> iterator;

        public HeadersIterator(java.util.Iterator<org.apache.kafka.common.header.Header> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public String next() {
            return iterator.next().key();
        }
    }
}