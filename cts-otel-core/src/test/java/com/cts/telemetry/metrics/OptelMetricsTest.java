package com.cts.telemetry.metrics;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enterprise-grade test class for OptelMetrics.
 * Tests metrics recording and histogram creation.
 */
@ExtendWith(MockitoExtension.class)
class OptelMetricsTest {

    @BeforeEach
    void setUp() {
        // Reset GlobalOpenTelemetry if needed
        // Note: GlobalOpenTelemetry is a singleton, so tests may affect each other
    }

    @Test
    void testRecordHttpDuration_WithValidAttributes_ShouldRecordSuccessfully() {
        // Arrange
        double duration = 1.5;
        Map<String, String> attributes = new HashMap<>();
        attributes.put("http.method", "GET");
        attributes.put("http.status_code", "200");
        attributes.put("http.route", "/api/test");

        // Act & Assert
        assertDoesNotThrow(() -> OptelMetrics.recordHttpDuration(duration, attributes),
                "Recording HTTP duration should not throw exception");
    }

    @Test
    void testRecordHttpDuration_WithNullAttributes_ShouldRecordSuccessfully() {
        // Arrange
        double duration = 0.5;

        // Act & Assert
        assertDoesNotThrow(() -> OptelMetrics.recordHttpDuration(duration, null),
                "Recording HTTP duration with null attributes should not throw exception");
    }

    @Test
    void testRecordHttpDuration_WithEmptyAttributes_ShouldRecordSuccessfully() {
        // Arrange
        double duration = 2.0;
        Map<String, String> attributes = new HashMap<>();

        // Act & Assert
        assertDoesNotThrow(() -> OptelMetrics.recordHttpDuration(duration, attributes),
                "Recording HTTP duration with empty attributes should not throw exception");
    }

    @Test
    void testRecordHttpDuration_WithZeroDuration_ShouldRecordSuccessfully() {
        // Arrange
        double duration = 0.0;
        Map<String, String> attributes = new HashMap<>();
        attributes.put("http.method", "POST");

        // Act & Assert
        assertDoesNotThrow(() -> OptelMetrics.recordHttpDuration(duration, attributes),
                "Recording zero duration should not throw exception");
    }

    @Test
    void testRecordHttpDuration_WithNegativeDuration_ShouldRecordSuccessfully() {
        // Arrange
        double duration = -1.0;
        Map<String, String> attributes = new HashMap<>();

        // Act & Assert
        // Note: OpenTelemetry may handle negative values differently
        assertDoesNotThrow(() -> OptelMetrics.recordHttpDuration(duration, attributes),
                "Recording negative duration should not throw exception");
    }

    @Test
    void testRecordHttpDuration_WithMultipleAttributes_ShouldRecordSuccessfully() {
        // Arrange
        double duration = 3.5;
        Map<String, String> attributes = new HashMap<>();
        attributes.put("http.method", "PUT");
        attributes.put("http.status_code", "201");
        attributes.put("http.route", "/api/users");
        attributes.put("http.scheme", "https");
        attributes.put("net.host.name", "example.com");

        // Act & Assert
        assertDoesNotThrow(() -> OptelMetrics.recordHttpDuration(duration, attributes),
                "Recording HTTP duration with multiple attributes should not throw exception");
    }

    @Test
    void testRecordHttpDuration_CalledMultipleTimes_ShouldRecordAllValues() {
        // Arrange
        Map<String, String> attributes = new HashMap<>();
        attributes.put("http.method", "GET");

        // Act & Assert
        assertDoesNotThrow(() -> {
            OptelMetrics.recordHttpDuration(1.0, attributes);
            OptelMetrics.recordHttpDuration(2.0, attributes);
            OptelMetrics.recordHttpDuration(3.0, attributes);
        }, "Recording multiple HTTP durations should not throw exception");
    }

    @Test
    void testRecordHttpDuration_WithVeryLargeDuration_ShouldRecordSuccessfully() {
        // Arrange
        double duration = Double.MAX_VALUE;
        Map<String, String> attributes = new HashMap<>();
        attributes.put("http.method", "GET");

        // Act & Assert
        assertDoesNotThrow(() -> OptelMetrics.recordHttpDuration(duration, attributes),
                "Recording very large duration should not throw exception");
    }

    @Test
    void testRecordHttpDuration_WithSpecialCharactersInAttributes_ShouldRecordSuccessfully() {
        // Arrange
        double duration = 1.0;
        Map<String, String> attributes = new HashMap<>();
        attributes.put("http.route", "/api/users/{id}");
        attributes.put("http.user_agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        // Act & Assert
        assertDoesNotThrow(() -> OptelMetrics.recordHttpDuration(duration, attributes),
                "Recording with special characters in attributes should not throw exception");
    }
}
