package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.tracing.OptelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
public class OptelWebClientInstrumentation implements BeanPostProcessor {

    private final OptelConfig config;

    public OptelWebClientInstrumentation(OptelConfig config) {
        this.config = config;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof WebClient.Builder && config.getTracing().isEnabled()
                && config.getTracing().getInstrument().getHttp().isEnabled()) {
            WebClient.Builder builder = (WebClient.Builder) bean;
            builder.filter(new TracingExchangeFilterFunction());
        }
        return bean;
    }

    private static class TracingExchangeFilterFunction implements ExchangeFilterFunction {
        @Override
        public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
            Span span = OptelTracer.startSpan(request.method().name() + " " + request.url(), SpanKind.CLIENT);

            try (Scope scope = span.makeCurrent()) {
                span.setAttribute(com.cts.telemetry.api.HttpAttributes.METHOD.key(), request.method().name());
                span.setAttribute(com.cts.telemetry.api.HttpAttributes.URL.key(), request.url().toString());

                ClientRequest.Builder requestBuilder = ClientRequest.from(request);
                com.cts.telemetry.tracing.TraceUtils.inject(Context.current(), requestBuilder,
                        (carrier, key, value) -> carrier.header(key, value));

                ClientRequest newRequest = requestBuilder.build();

                return next.exchange(newRequest)
                        .doOnSuccess(response -> {
                            span.setAttribute(com.cts.telemetry.api.HttpAttributes.STATUS_CODE.key(),
                                    response.statusCode().value());
                            span.end();
                        })
                        .doOnError(error -> {
                            span.recordException(error);
                            span.setStatus(StatusCode.ERROR, error.getMessage());
                            span.end();
                        });
            }
        }
    }
}
