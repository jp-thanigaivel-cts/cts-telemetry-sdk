package com.cts.telemetry.init;

import com.cts.telemetry.config.ExporterType;
import com.cts.telemetry.config.OptelConfig;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public class OptelInitializer {

        @Getter
        private static OpenTelemetrySdk openTelemetrySdk;

        @Getter
        private static OptelConfig config;

        public static synchronized void initialize(OptelConfig optelConfig) {
                config = optelConfig;
                if (!config.isEnabled()) {
                        log.debug("OpenTelemetry SDK is disabled");
                        return;
                }
                if (openTelemetrySdk != null) {
                        log.debug("OpenTelemetry SDK already initialized");
                        return;
                }

                log.info("Initializing OpenTelemetry SDK for service: {}", config.getServiceName());

                Resource resource = Resource.getDefault()
                                .merge(Resource.create(Attributes.builder()
                                                .put(ResourceAttributes.SERVICE_NAME, config.getServiceName())
                                                .put(com.cts.telemetry.api.AppAttributes.TELEMETRY_SDK_LANGUAGE.key(),
                                                                "java")
                                                .put(com.cts.telemetry.api.AppAttributes.TELEMETRY_SDK_NAME.key(),
                                                                "cts-telemetry-sdk")
                                                .put(com.cts.telemetry.api.AppAttributes.TELEMETRY_SDK_VERSION.key(),
                                                                "1.0.0")
                                                .build()));

                SdkMeterProviderBuilder meterProviderBuilder = SdkMeterProvider.builder()
                                .setResource(resource);

                Duration interval = Duration.ofSeconds(config.getExportIntervalSeconds());

                // Configure exporters based on config
                if (config.getExporter() == ExporterType.GRPC
                                || config.getExporter() == ExporterType.BOTH) {
                        meterProviderBuilder.registerMetricReader(
                                        PeriodicMetricReader.builder(
                                                        OtlpGrpcMetricExporter.builder()
                                                                        .setEndpoint(config.getEndpoint())
                                                                        .setTimeout(Duration.ofSeconds(30))
                                                                        .build())
                                                        .setInterval(interval)
                                                        .build());
                }

                if (config.getExporter() == ExporterType.LOGGING
                                || config.getExporter() == ExporterType.BOTH) {
                        meterProviderBuilder.registerMetricReader(
                                        PeriodicMetricReader.builder(
                                                        LoggingMetricExporter.create())
                                                        .setInterval(interval)
                                                        .build());
                }

                io.opentelemetry.sdk.OpenTelemetrySdkBuilder sdkBuilder = OpenTelemetrySdk.builder()
                                .setMeterProvider(meterProviderBuilder.build());

                // Tracing Initialization
                if (config.getTracing().isEnabled()) {
                        io.opentelemetry.sdk.trace.SdkTracerProviderBuilder tracerProviderBuilder = io.opentelemetry.sdk.trace.SdkTracerProvider
                                        .builder()
                                        .setResource(resource);

                        if (config.getExporter() == ExporterType.GRPC
                                        || config.getExporter() == ExporterType.BOTH) {
                                tracerProviderBuilder.addSpanProcessor(
                                                io.opentelemetry.sdk.trace.export.BatchSpanProcessor.builder(
                                                                io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
                                                                                .builder()
                                                                                .setEndpoint(config.getEndpoint())
                                                                                .setTimeout(Duration.ofSeconds(30))
                                                                                .build())
                                                                .build());
                        }

                        if (config.getExporter() == ExporterType.LOGGING
                                        || config.getExporter() == ExporterType.BOTH) {
                                tracerProviderBuilder.addSpanProcessor(
                                                io.opentelemetry.sdk.trace.export.SimpleSpanProcessor.create(
                                                                io.opentelemetry.exporter.logging.LoggingSpanExporter
                                                                                .create()));
                        }

                        sdkBuilder.setTracerProvider(tracerProviderBuilder.build());
                        sdkBuilder.setPropagators(io.opentelemetry.context.propagation.ContextPropagators.create(
                                        io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
                                                        .getInstance()));
                }

                try {
                        openTelemetrySdk = sdkBuilder.buildAndRegisterGlobal();
                        log.info("OpenTelemetry SDK initialized successfully for service: {} with exporter: {}, Metrics: {}, Tracing: {}",
                                        config.getServiceName(), config.getExporter(), config.getMetrics().isEnabled(),
                                        config.getTracing().isEnabled());
                } catch (IllegalStateException e) {
                        // GlobalOpenTelemetry.set has already been called.
                        // This can happen in tests or if another library initialized it.
                        // We will try to use the global instance if possible, or just log and continue.
                        log.warn("GlobalOpenTelemetry already initialized: {}", e.getMessage());
                        // If we can't set it, we can't easily get the SDK instance back if it wasn't
                        // stored.
                        // But we can continue without crashing.
                }
        }

}
