package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.tracing.OptelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OptelHttpClientInstrumentation implements BeanPostProcessor {

    private final OptelConfig config;

    public OptelHttpClientInstrumentation(OptelConfig config) {
        this.config = config;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof RestTemplate && config.getTracing().isEnabled()
                && config.getTracing().getInstrument().getHttp().isEnabled()) {
            RestTemplate restTemplate = (RestTemplate) bean;
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
            interceptors.add(new TracingInterceptor());
            restTemplate.setInterceptors(interceptors);
        }
        return bean;
    }

    private static class TracingInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
                throws IOException {
            Span span = OptelTracer.startSpan(request.getMethod().name() + " " + request.getURI(), SpanKind.CLIENT);

            try (Scope scope = span.makeCurrent()) {
                span.setAttribute(com.cts.telemetry.api.HttpAttributes.METHOD.key(), request.getMethod().name());
                span.setAttribute(com.cts.telemetry.api.HttpAttributes.URL.key(), request.getURI().toString());

                com.cts.telemetry.tracing.TraceUtils.inject(Context.current(), request,
                        (carrier, key, value) -> carrier.getHeaders().set(key, value));

                ClientHttpResponse response = execution.execute(request, body);
                span.setAttribute(com.cts.telemetry.api.HttpAttributes.STATUS_CODE.key(),
                        response.getStatusCode().value());
                return response;
            } catch (IOException e) {
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, e.getMessage());
                throw e;
            } finally {
                span.end();
            }
        }
    }
}
