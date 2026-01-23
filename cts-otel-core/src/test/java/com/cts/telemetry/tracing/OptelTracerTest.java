package com.cts.telemetry.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Enterprise-grade test class for OptelTracer.
 * Tests span creation and tracing utilities.
 */
@ExtendWith(MockitoExtension.class)
class OptelTracerTest {

    private Tracer tracer;

    @BeforeEach
    void setUp() {
        tracer = OptelTracer.getTracer();
    }

    @Test
    void testGetTracer_ShouldReturnNonNullTracer() {
        // Act
        Tracer result = OptelTracer.getTracer();

        // Assert
        assertNotNull(result, "Tracer should not be null");
    }

    @Test
    void testStartSpan_WithServerKind_ShouldCreateSpan() {
        // Arrange
        String spanName = "test-server-span";

        // Act
        Span span = OptelTracer.startSpan(spanName, SpanKind.SERVER);

        // Assert
        assertNotNull(span, "Span should not be null");
        span.end();
    }

    @Test
    void testStartSpan_WithClientKind_ShouldCreateSpan() {
        // Arrange
        String spanName = "test-client-span";

        // Act
        Span span = OptelTracer.startSpan(spanName, SpanKind.CLIENT);

        // Assert
        assertNotNull(span, "Span should not be null");
        span.end();
    }

    @Test
    void testStartSpan_WithInternalKind_ShouldCreateSpan() {
        // Arrange
        String spanName = "test-internal-span";

        // Act
        Span span = OptelTracer.startSpan(spanName, SpanKind.INTERNAL);

        // Assert
        assertNotNull(span, "Span should not be null");
        span.end();
    }

    @Test
    void testStartSpan_WithParentContext_ShouldCreateChildSpan() {
        // Arrange
        String parentSpanName = "parent-span";
        String childSpanName = "child-span";
        Span parentSpan = OptelTracer.startSpan(parentSpanName, SpanKind.SERVER);
        Context parentContext = Context.current().with(parentSpan);

        // Act
        Span childSpan = OptelTracer.startSpan(childSpanName, SpanKind.INTERNAL, parentContext);

        // Assert
        assertNotNull(childSpan, "Child span should not be null");
        childSpan.end();
        parentSpan.end();
    }

    @Test
    void testTraceCallable_WithSuccessfulTask_ShouldReturnResult() throws Exception {
        // Arrange
        String spanName = "test-callable-span";
        String expectedResult = "success";
        Callable<String> task = () -> expectedResult;

        // Act
        String result = OptelTracer.traceCallable(spanName, task);

        // Assert
        assertEquals(expectedResult, result, "Callable should return expected result");
    }

    @Test
    void testTraceCallable_WithException_ShouldPropagateException() {
        // Arrange
        String spanName = "test-callable-exception";
        String errorMessage = "Test exception";
        Callable<String> task = () -> {
            throw new RuntimeException(errorMessage);
        };

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            OptelTracer.traceCallable(spanName, task);
        }, "Exception should be propagated");

        assertEquals(errorMessage, exception.getMessage(), "Exception message should match");
    }

    @Test
    void testTraceCallable_WithNullReturn_ShouldReturnNull() throws Exception {
        // Arrange
        String spanName = "test-callable-null";
        Callable<String> task = () -> null;

        // Act
        String result = OptelTracer.traceCallable(spanName, task);

        // Assert
        assertNull(result, "Callable should return null");
    }

    @Test
    void testTraceRunnable_WithSuccessfulTask_ShouldExecute() {
        // Arrange
        String spanName = "test-runnable-span";
        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable task = () -> executed.set(true);

        // Act
        OptelTracer.traceRunnable(spanName, task);

        // Assert
        assertTrue(executed.get(), "Runnable should have been executed");
    }

    @Test
    void testTraceRunnable_WithException_ShouldPropagateException() {
        // Arrange
        String spanName = "test-runnable-exception";
        String errorMessage = "Test runnable exception";
        Runnable task = () -> {
            throw new RuntimeException(errorMessage);
        };

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            OptelTracer.traceRunnable(spanName, task);
        }, "Exception should be propagated");

        assertEquals(errorMessage, exception.getMessage(), "Exception message should match");
    }

    @Test
    void testTraceRunnable_WithMultipleOperations_ShouldExecuteAll() {
        // Arrange
        String spanName = "test-runnable-multiple";
        AtomicBoolean step1 = new AtomicBoolean(false);
        AtomicBoolean step2 = new AtomicBoolean(false);
        AtomicBoolean step3 = new AtomicBoolean(false);

        Runnable task = () -> {
            step1.set(true);
            step2.set(true);
            step3.set(true);
        };

        // Act
        OptelTracer.traceRunnable(spanName, task);

        // Assert
        assertTrue(step1.get() && step2.get() && step3.get(),
                "All operations should have been executed");
    }

    @Test
    void testTraceCallable_WithComplexObject_ShouldReturnObject() throws Exception {
        // Arrange
        String spanName = "test-callable-complex";
        ComplexObject expected = new ComplexObject("test", 123);
        Callable<ComplexObject> task = () -> expected;

        // Act
        ComplexObject result = OptelTracer.traceCallable(spanName, task);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertEquals(expected.getName(), result.getName(), "Object name should match");
        assertEquals(expected.getValue(), result.getValue(), "Object value should match");
    }

    // Helper class for testing
    private static class ComplexObject {
        private final String name;
        private final int value;

        public ComplexObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
