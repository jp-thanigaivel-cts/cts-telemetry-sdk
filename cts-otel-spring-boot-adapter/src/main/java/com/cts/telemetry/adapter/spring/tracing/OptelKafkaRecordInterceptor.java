package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.api.SpanAttributes;
import com.cts.telemetry.tracing.OptelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jspecify.annotations.Nullable;
import org.springframework.kafka.listener.RecordInterceptor;

import java.nio.charset.StandardCharsets;

@Slf4j
public class OptelKafkaRecordInterceptor implements RecordInterceptor<Object, Object> {

    private final ThreadLocal<Span> spanThreadLocal = new ThreadLocal<>();
    private final ThreadLocal<Scope> scopeThreadLocal = new ThreadLocal<>();


    @Override
    public @Nullable ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        log.info("onConsume is started");
        Context extractedContext = com.cts.telemetry.tracing.TraceUtils.extract(
                Context.current(),
                record.headers(),
                new HeadersGetter());

        Span span = OptelTracer.startSpan("Kafka Consume " + record.topic(), SpanKind.CONSUMER, extractedContext);

        span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_SYSTEM.key(), "kafka");
        span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_DESTINATION.key(), record.topic());
        if (consumer.groupMetadata() != null && consumer.groupMetadata().groupId() != null) {
            span.setAttribute(SpanAttributes.MESSAGING_KAFKA_CONSUMER_GROUP_NAME.key(), consumer.groupMetadata().groupId());
        }
        span.setAttribute(SpanAttributes.MESSAGING_OPERATION_NAME.key(), "process");
        span.setAttribute(SpanAttributes.MESSAGING_OPERATION_TYPE.key(), "process");
        if (record.key() != null) {
            span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_KAFKA_MESSAGE_KEY.key(), record.key().toString());
        }
        span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_KAFKA_PARTITION.key(), record.partition());
        span.setAttribute(com.cts.telemetry.api.SpanAttributes.MESSAGING_KAFKA_OFFSET.key(), record.offset());

        spanThreadLocal.set(span);
        scopeThreadLocal.set(span.makeCurrent());
        return record;
    }

    @Override
    public void success(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        cleanup();
    }

    @Override
    public void failure(ConsumerRecord<Object, Object> record, Exception exception, Consumer<Object, Object> consumer) {
        Span span = spanThreadLocal.get();
        if (span != null) {
            span.recordException(exception);
        }
        cleanup();
    }

    /*@Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
       log.info("afterRecord is started");
       Scope scope = scopeThreadLocal.get();
       if (scope != null) {
          scope.close();
          scopeThreadLocal.remove();
       }
       Span.current().end();
    }*/

    private void cleanup() {
        Scope scope = scopeThreadLocal.get();
        if (scope != null) {
            scope.close();
            scopeThreadLocal.remove();
        }
        Span span = spanThreadLocal.get();
        if (span != null) {
            span.end();
            spanThreadLocal.remove();
        }
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