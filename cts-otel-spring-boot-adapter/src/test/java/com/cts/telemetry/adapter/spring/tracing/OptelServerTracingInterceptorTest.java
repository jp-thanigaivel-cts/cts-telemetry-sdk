package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.config.TracingConfig;
import com.cts.telemetry.config.InstrumentConfig;
import com.cts.telemetry.config.tracing.HttpConfig;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OptelServerTracingInterceptorTest {

    @Mock
    private OptelConfig config;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    @Test
    public void testTraceIdPropagation() {
        // Setup config
        TracingConfig tracingConfig = mock(TracingConfig.class);
        InstrumentConfig instrumentConfig = mock(InstrumentConfig.class);
        HttpConfig httpConfig = mock(HttpConfig.class);

        when(config.getTracing()).thenReturn(tracingConfig);
        when(tracingConfig.isEnabled()).thenReturn(true);
        when(tracingConfig.getInstrument()).thenReturn(instrumentConfig);
        when(instrumentConfig.getHttp()).thenReturn(httpConfig);
        when(httpConfig.isEnabled()).thenReturn(true);

        // Setup request
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost/test"));

        OptelServerTracingInterceptor interceptor = new OptelServerTracingInterceptor(config);

        // Execute preHandle
        interceptor.preHandle(request, response, new Object());

        // Verify Trace ID is set in response header
        verify(response).setHeader(eq("traceId"), any(String.class));
    }
}
