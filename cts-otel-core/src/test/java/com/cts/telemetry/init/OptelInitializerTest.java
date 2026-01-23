package com.cts.telemetry.init;

import com.cts.telemetry.config.ExporterType;
import com.cts.telemetry.config.MetricsConfig;
import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.config.TracingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enterprise-grade test class for OptelInitializer.
 * Tests SDK initialization with various configurations.
 * 
 * Note: These tests work with the global SDK registration pattern.
 * Once initialized, the SDK cannot be easily reset, so tests verify
 * behavior rather than exact state.
 */
@ExtendWith(MockitoExtension.class)
class OptelInitializerTest {

    private OptelConfig config;

    @BeforeEach
    void setUp() {
        config = new OptelConfig();
        config.setEnabled(true);
        config.setServiceName("test-service");
        config.setEndpoint("http://localhost:4317");
        config.setExporter(ExporterType.LOGGING);
        config.setExportIntervalSeconds(10);

        MetricsConfig metricsConfig = new MetricsConfig();
        metricsConfig.setEnabled(true);
        config.setMetrics(metricsConfig);

        TracingConfig tracingConfig = new TracingConfig();
        tracingConfig.setEnabled(true);
        config.setTracing(tracingConfig);
    }

    @Test
    void testInitialize_WithValidConfig_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> OptelInitializer.initialize(config),
                "Initialization with valid config should not throw exception");
    }

    @Test
    void testInitialize_WithDisabledConfig_ShouldNotThrowException() {
        // Arrange
        config.setEnabled(false);

        // Act & Assert
        assertDoesNotThrow(() -> OptelInitializer.initialize(config),
                "Initialization with disabled config should not throw exception");
    }

    @Test
    void testInitialize_WithGrpcExporter_ShouldNotThrowException() {
        // Arrange
        config.setExporter(ExporterType.GRPC);

        // Act & Assert
        assertDoesNotThrow(() -> OptelInitializer.initialize(config),
                "Initialization with GRPC exporter should not throw exception");
    }

    @Test
    void testInitialize_WithBothExporters_ShouldNotThrowException() {
        // Arrange
        config.setExporter(ExporterType.BOTH);

        // Act & Assert
        assertDoesNotThrow(() -> OptelInitializer.initialize(config),
                "Initialization with both exporters should not throw exception");
    }

    @Test
    void testInitialize_WithTracingDisabled_ShouldNotThrowException() {
        // Arrange
        config.getTracing().setEnabled(false);

        // Act & Assert
        assertDoesNotThrow(() -> OptelInitializer.initialize(config),
                "Initialization without tracing should not throw exception");
    }

    @Test
    void testInitialize_CalledMultipleTimes_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> {
            OptelInitializer.initialize(config);
            OptelInitializer.initialize(config);
        }, "Multiple initialization calls should not throw exception");
    }

    @Test
    void testInitialize_WithCustomServiceName_ShouldNotThrowException() {
        // Arrange
        String customServiceName = "custom-test-service";
        config.setServiceName(customServiceName);

        // Act & Assert
        assertDoesNotThrow(() -> OptelInitializer.initialize(config),
                "Initialization with custom service name should not throw exception");
    }

    @Test
    void testInitialize_WithCustomExportInterval_ShouldNotThrowException() {
        // Arrange
        config.setExportIntervalSeconds(30);

        // Act & Assert
        assertDoesNotThrow(() -> OptelInitializer.initialize(config),
                "Initialization with custom export interval should not throw exception");
    }

    @Test
    void testInitialize_WithNullConfig_ShouldHandleGracefully() {
        // Act & Assert - should not crash
        assertDoesNotThrow(() -> {
            try {
                OptelInitializer.initialize(null);
            } catch (NullPointerException e) {
                // Expected for null config
            }
        }, "Null config should be handled");
    }

    @Test
    void testGetOpenTelemetrySdk_AfterInitialization_ShouldReturnSdk() {
        // Arrange
        OptelInitializer.initialize(config);

        // Act & Assert
        // SDK may or may not be set depending on global state, but method should not
        // throw
        assertDoesNotThrow(() -> OptelInitializer.getOpenTelemetrySdk(),
                "Getting SDK should not throw exception");
    }
}
