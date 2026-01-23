package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.tracing.OptelTracer;
import com.cts.telemetry.tracing.TraceUtils;
import io.grpc.*;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.interceptor.GlobalClientInterceptorConfigurer;
import net.devh.boot.grpc.server.interceptor.GlobalServerInterceptorConfigurer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnClass({ ServerInterceptor.class, ClientInterceptor.class, GlobalClientInterceptorConfigurer.class })
@ConditionalOnProperty(prefix = "optel.tracing", name = "enabled", havingValue = "true")
public class OptelGrpcInstrumentation {

    @Bean
    @ConditionalOnProperty(prefix = "optel.tracing.instrument.grpc", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GlobalClientInterceptorConfigurer globalClientInterceptorConfigurer(OptelConfig config) {
        return registry -> {
            if (config.getTracing().getInstrument().getGrpc().isEnabled()) {
                log.info("Registering Optel gRPC Client Interceptor");
                registry.add(new TracingClientInterceptor());
            }
        };
    }

    @Bean
    @ConditionalOnProperty(prefix = "optel.tracing.instrument.grpc", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GlobalServerInterceptorConfigurer globalServerInterceptorConfigurer(OptelConfig config) {
        return registry -> {
            if (config.getTracing().getInstrument().getGrpc().isEnabled()) {
                log.info("Registering Optel gRPC Server Interceptor");
                registry.add(new TracingServerInterceptor());
            }
        };
    }

    static class TracingClientInterceptor implements ClientInterceptor {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    Span span = OptelTracer.startSpan("gRPC Client: " + method.getFullMethodName(), SpanKind.CLIENT);
                    try (Scope scope = span.makeCurrent()) {
                        TraceUtils.inject(Context.current(), headers, new GrpcHeaderSetter());
                        super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                                responseListener) {
                            @Override
                            public void onClose(Status status, Metadata trailers) {
                                if (!status.isOk()) {
                                    span.setStatus(StatusCode.ERROR, status.getDescription());
                                    if (status.getCause() != null) {
                                        span.recordException(status.getCause());
                                    }
                                }
                                span.end();
                                super.onClose(status, trailers);
                            }
                        }, headers);
                    }
                }
            };
        }
    }

    static class TracingServerInterceptor implements ServerInterceptor {
        @Override
        public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers,
                ServerCallHandler<ReqT, RespT> next) {
            Context extractedContext = TraceUtils.extract(Context.current(), headers, new GrpcHeaderGetter());

            Span span = OptelTracer.startSpan("gRPC Server: " + call.getMethodDescriptor().getFullMethodName(),
                    SpanKind.SERVER, extractedContext);

            try (Scope scope = span.makeCurrent()) {
                return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(
                        next.startCall(new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                            @Override
                            public void close(Status status, Metadata trailers) {
                                if (!status.isOk()) {
                                    span.setStatus(StatusCode.ERROR, status.getDescription());
                                    if (status.getCause() != null) {
                                        span.recordException(status.getCause());
                                    }
                                }
                                span.end();
                                super.close(status, trailers);
                            }
                        }, headers)) {
                    // Hook for listener events if needed
                };
            }
        }
    }

    static class GrpcHeaderSetter implements TextMapSetter<Metadata> {
        @Override
        public void set(Metadata carrier, String key, String value) {
            Metadata.Key<String> metadataKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
            carrier.put(metadataKey, value);
        }
    }

    static class GrpcHeaderGetter implements TextMapGetter<Metadata> {
        @Override
        public Iterable<String> keys(Metadata carrier) {
            return carrier.keys();
        }

        @Override
        public String get(Metadata carrier, String key) {
            Metadata.Key<String> metadataKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
            if (carrier.containsKey(metadataKey)) {
                return carrier.get(metadataKey);
            }
            return null;
        }
    }
}
