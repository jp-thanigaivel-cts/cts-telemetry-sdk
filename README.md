# CTS OpenTelemetry SDK

## Overview
Enterprise-grade OpenTelemetry SDK for Java applications providing automatic instrumentation, metrics collection, and distributed tracing capabilities. Designed for production use with support for Spring Boot, Jersey, and standalone Java applications.

## Features

- ✅ **Zero-Code Instrumentation** - Auto-configuration for Spring Boot
- ✅ **Comprehensive Tracing** - HTTP, gRPC, Database, Cache, Kafka, JMS
- ✅ **Metrics Collection** - JVM metrics, HTTP metrics, custom metrics
- ✅ **Multiple Exporters** - OTLP (gRPC), Logging, or Both
- ✅ **Trace Propagation** - W3C Trace Context standard
- ✅ **Production-Ready** - Enterprise-grade testing and documentation
- ✅ **Framework Support** - Spring Boot, Jersey (JAX-RS)

## Architecture

```
cts-telemetry-sdk
├── cts-otel-core                    # Core SDK components
│   ├── Configuration Management
│   ├── SDK Initialization
│   ├── Metrics Management
│   └── Tracing Utilities
├── cts-otel-spring-boot-adapter     # Spring Boot integration
│   ├── Auto-Configuration
│   ├── HTTP Server/Client Instrumentation
│   ├── Database Instrumentation
│   ├── Cache Instrumentation
│   ├── gRPC Instrumentation
│   ├── Kafka Instrumentation
│   └── Internal Method Tracing
├── cts-otel-jersey-adapter          # Jersey (JAX-RS) integration
│   ├── HTTP Server Instrumentation
│   └── Metrics Collection
└── examples                          # Example applications
    ├── demo-app-spring-boot         # Spring Boot demo
    └── demo-app-spring-boot-2       # Multi-service demo
```

## Modules

- **Tracing**: Spans will have `status_code: ERROR` and `exception` events.
- Test error handling:
  ```bash
  curl -v http://localhost:8080/orders/error
  ```
2.  **Run the Demo App**

    ```bash
    ./gradlew :examples:demo-app-spring-boot:bootRun
    ```

    The app will start on `http://localhost:8080`.

## Verifying Metrics

1.  **Generate Traffic**

    ```bash
    # Create an order
    curl -X POST -H "Content-Type: application/json" -d '{"item":"Laptop","price":1200.0}' http://localhost:8080/orders

    # Get order
    curl -H "X-Tenant-Id: T123" http://localhost:8080/orders/1
    
    # Trigger Error (to see error attributes)
    curl http://localhost:8080/orders/error
    ```

2.  **Check Logs**

    The demo app is configured with `exporter: BOTH`, so you will see metrics printed in the console logs (DEBUG level for OTel logging exporter might be needed, or standard output depending on impl).
    
    Look for `http.server.duration` with attributes:
    - `http.method`, `http.status_code`, `http.route`
    - `service.name`, `tenant.id`, `api.name`
    - `error.type`, `error.message` (on errors)

## Configuration

The SDK is configured via `application.yml`:

```yaml
optel:
  enabled: true
  metrics:
    enabled: true
    export-interval-seconds: 5
  service:
    name: demo-order-service
  otlp:
    endpoint: http://localhost:4317
  exporter: BOTH # Options: GRPC, LOGGING, BOTH
```
