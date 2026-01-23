# CTS OpenTelemetry Core Module

## Overview
The `cts-otel-core` module provides the foundational components for OpenTelemetry instrumentation in CTS applications. It includes configuration management, SDK initialization, metrics publishing, and tracing utilities.

## Features

- ✅ **SDK Initialization** - Automated OpenTelemetry SDK setup
- ✅ **Metrics Management** - HTTP metrics, JVM metrics, custom metrics
- ✅ **Tracing Utilities** - Span creation, context propagation
- ✅ **Flexible Configuration** - YAML/Properties-based configuration
- ✅ **Multiple Exporters** - GRPC, Logging, or Both
- ✅ **Memory Metrics** - Automatic JVM memory metrics collection

## Architecture

```
cts-otel-core
├── config/              # Configuration classes
│   ├── OptelConfig      # Main configuration
│   ├── MetricsConfig    # Metrics configuration
│   ├── TracingConfig    # Tracing configuration
│   └── tracing/         # Instrumentation configs
├── init/                # SDK initialization
│   └── OptelInitializer # OpenTelemetry SDK setup
├── metrics/             # Metrics management
│   ├── OptelMetrics     # Metrics API
│   └── MemoryMetricsPublisher # JVM metrics
└── tracing/             # Tracing utilities
    ├── OptelTracer      # Tracer API
    └── TraceUtils       # Trace utilities
```

## Installation

### Gradle
```kotlin
dependencies {
    implementation("com.cts:cts-otel-core:1.0.0")
}
```

### Maven
```xml
<dependency>
    <groupId>com.cts</groupId>
    <artifactId>cts-otel-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration

### Basic Configuration

```yaml
optel:
  enabled: true
  serviceName: my-service
  endpoint: http://localhost:4317
  exporter: BOTH  # GRPC, LOGGING, or BOTH
  export-interval-seconds: 15
  
  metrics:
    enabled: true
    memory:
      enabled: true
      pushIntervalMs: 5000
  
  tracing:
    enabled: true
```

### Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `optel.enabled` | boolean | `false` | Enable/disable OpenTelemetry |
| `optel.serviceName` | String | `unknown-service` | Service name for telemetry |
| `optel.endpoint` | String | `http://localhost:4317` | OTLP endpoint |
| `optel.exporter` | Enum | `LOGGING` | Exporter type (GRPC/LOGGING/BOTH) |
| `optel.export-interval-seconds` | int | `15` | Export interval in seconds |
| `optel.metrics.enabled` | boolean | `true` | Enable metrics collection |
| `optel.metrics.memory.enabled` | boolean | `true` | Enable JVM memory metrics |
| `optel.metrics.memory.pushIntervalMs` | long | `5000` | Memory metrics push interval |
| `optel.tracing.enabled` | boolean | `true` | Enable tracing |

## Usage

### SDK Initialization

```java
import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.init.OptelInitializer;

// Initialize SDK
OptelConfig config = new OptelConfig();
config.setEnabled(true);
config.setServiceName("my-service");
OptelInitializer.initialize(config);
```

### Recording Metrics

```java
import com.cts.telemetry.metrics.OptelMetrics;
import java.util.Map;

// Record HTTP duration
Map<String, String> attributes = Map.of(
    "http.method", "GET",
    "http.status_code", "200",
    "http.route", "/api/users"
);
OptelMetrics.recordHttpDuration(1.5, attributes);
```

### Creating Spans

```java
import com.cts.telemetry.tracing.OptelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;

// Create a span
Span span = OptelTracer.startSpan("my-operation", SpanKind.INTERNAL);
try {
    // Your code here
    span.setAttribute("custom.attribute", "value");
} finally {
    span.end();
}
```

### Tracing Callables and Runnables

```java
import com.cts.telemetry.tracing.OptelTracer;

// Trace a Callable
String result = OptelTracer.traceCallable("fetch-data", () -> {
    return fetchDataFromDatabase();
});

// Trace a Runnable
OptelTracer.traceRunnable("process-data", () -> {
    processData();
});
```

## Exporter Types

### GRPC Exporter
Exports telemetry data to an OTLP-compatible backend (e.g., Jaeger, Zipkin, OpenTelemetry Collector).

```yaml
optel:
  exporter: GRPC
  endpoint: http://localhost:4317
```

### Logging Exporter
Logs telemetry data to console (useful for development/debugging).

```yaml
optel:
  exporter: LOGGING
```

### Both Exporters
Exports to both GRPC and Logging simultaneously.

```yaml
optel:
  exporter: BOTH
  endpoint: http://localhost:4317
```

## Memory Metrics

The module automatically collects JVM memory metrics:

- `jvm.memory.used` - Memory usage by pool
- `jvm.memory.committed` - Committed memory
- `jvm.memory.max` - Maximum memory
- `jvm.gc.duration` - GC duration
- `jvm.thread.count` - Thread count
- `jvm.class.loaded` - Loaded classes
- `jvm.class.unloaded` - Unloaded classes

## Testing

Run tests:
```bash
./gradlew :cts-otel-core:test
```

Generate coverage report:
```bash
./gradlew :cts-otel-core:jacocoTestReport
```

## Dependencies

- OpenTelemetry SDK
- OpenTelemetry API
- OpenTelemetry Exporters (OTLP, Logging)
- Lombok
- SLF4J

## License

Copyright © 2025 CTS. All rights reserved.
