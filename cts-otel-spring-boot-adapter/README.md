# CTS OpenTelemetry Spring Boot Adapter

## Overview
The `cts-otel-spring-boot-adapter` module provides seamless OpenTelemetry integration for Spring Boot applications with auto-configuration and automatic instrumentation for common components.

## Features

- ✅ **Auto-Configuration** - Zero-code Spring Boot integration
- ✅ **HTTP Server Instrumentation** - Automatic SERVER span creation
- ✅ **HTTP Client Instrumentation** - RestTemplate & WebClient tracing
- ✅ **Database Instrumentation** - DataSource/JDBC tracing
- ✅ **Cache Instrumentation** - Spring Cache tracing
- ✅ **gRPC Instrumentation** - gRPC client/server tracing
- ✅ **Kafka Instrumentation** - Kafka producer/consumer tracing
- ✅ **Internal Tracing** - AspectJ-based method tracing
- ✅ **Trace Propagation** - W3C Trace Context propagation

## Installation

### Gradle
```kotlin
dependencies {
    implementation("com.cts:cts-otel-spring-boot-adapter:1.0.0")
}
```

### Maven
```xml
<dependency>
    <groupId>com.cts</groupId>
    <artifactId>cts-otel-spring-boot-adapter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration

### Complete Configuration Example

```yaml
optel:
  enabled: true
  serviceName: my-spring-boot-app
  endpoint: http://localhost:4317
  exporter: BOTH
  export-interval-seconds: 15
  
  metrics:
    enabled: true
    memory:
      enabled: true
      pushIntervalMs: 5000
  
  tracing:
    enabled: true
    instrument:
      http:
        enabled: true
      db:
        enabled: true
        capture-sql: true
        capture-params: false
      cache:
        enabled: true
      grpc:
        enabled: true
      kafka:
        producer: true
        consumer: true
      jms:
        enabled: true
      internal:
        enabled: true
        packages: [com.example.service, com.example.repository]
```

### Instrumentation Configuration

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `optel.tracing.instrument.http.enabled` | boolean | `true` | Enable HTTP instrumentation |
| `optel.tracing.instrument.db.enabled` | boolean | `true` | Enable database instrumentation |
| `optel.tracing.instrument.db.capture-sql` | boolean | `true` | Capture SQL statements |
| `optel.tracing.instrument.db.capture-params` | boolean | `false` | Capture SQL parameters |
| `optel.tracing.instrument.cache.enabled` | boolean | `true` | Enable cache instrumentation |
| `optel.tracing.instrument.grpc.enabled` | boolean | `false` | Enable gRPC instrumentation |
| `optel.tracing.instrument.kafka.producer` | boolean | `false` | Enable Kafka producer tracing |
| `optel.tracing.instrument.kafka.consumer` | boolean | `false` | Enable Kafka consumer tracing |
| `optel.tracing.instrument.internal.enabled` | boolean | `false` | Enable internal method tracing |
| `optel.tracing.instrument.internal.packages` | List<String> | `[]` | Packages to instrument |

## Instrumentation Types

### 1. HTTP Server Instrumentation

Automatically creates SERVER spans for incoming HTTP requests.

**Span Attributes:**
- `http.method` - HTTP method (GET, POST, etc.)
- `http.route` - Request path
- `http.status_code` - Response status code
- `http.scheme` - HTTP or HTTPS
- `net.host.name` - Host name

**Example:**
```java
@RestController
public class UserController {
    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        // Automatic SERVER span created
        return userService.findById(id);
    }
}
```

### 2. HTTP Client Instrumentation

Instruments RestTemplate and WebClient for outbound HTTP calls.

**RestTemplate Example:**
```java
@Configuration
public class AppConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
        // Automatically instrumented
    }
}
```

**WebClient Example:**
```java
@Service
public class ExternalService {
    private final WebClient webClient;
    
    public ExternalService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        // Automatically instrumented
    }
}
```

### 3. Database Instrumentation

Instruments DataSource for database operations.

**Features:**
- Automatic span creation for SQL queries
- SQL statement capture (configurable)
- Parameter capture (configurable)
- Connection pool monitoring

**Example:**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // All database operations automatically traced
    List<User> findByEmail(String email);
}
```

### 4. Cache Instrumentation

Instruments Spring Cache operations.

**Supported Operations:**
- `get` - Cache retrieval
- `put` - Cache insertion
- `evict` - Cache eviction
- `clear` - Cache clearing

**Example:**
```java
@Service
public class UserService {
    @Cacheable("users")
    public User findById(Long id) {
        // Cache operations automatically traced
        return userRepository.findById(id);
    }
}
```

### 5. gRPC Instrumentation

Instruments gRPC clients and servers.

**Configuration:**
```yaml
optel:
  tracing:
    instrument:
      grpc:
        enabled: true
```

**Example:**
```java
@Service
public class GrpcClientService {
    @GrpcClient("my-service")
    private MyServiceBlockingStub stub;
    
    public Response callService() {
        // Automatic gRPC CLIENT span
        return stub.myMethod(request);
    }
}
```

### 6. Kafka Instrumentation

Instruments Kafka producers and consumers.

**Configuration:**
```yaml
optel:
  tracing:
    instrument:
      kafka:
        producer: true
        consumer: true
```

**Producer Example:**
```java
@Service
public class EventPublisher {
    @Autowired
    private KafkaTemplate<String, Event> kafkaTemplate;
    
    public void publishEvent(Event event) {
        // Automatic PRODUCER span
        kafkaTemplate.send("events", event);
    }
}
```

### 7. Internal Tracing

Instruments internal methods using AspectJ.

**Configuration:**
```yaml
optel:
  tracing:
    instrument:
      internal:
        enabled: true
        packages: [com.example.service]
```

**Example:**
```java
package com.example.service;

@Service
public class BusinessService {
    public void processOrder(Order order) {
        // Automatic INTERNAL span created
        validateOrder(order);
        calculateTotal(order);
    }
    
    private void validateOrder(Order order) {
        // Automatic INTERNAL span created
    }
}
```

## Auto-Configuration

The adapter provides auto-configuration beans:

- `OptelAutoConfiguration` - Main configuration
- `OptelTracingAutoConfiguration` - Tracing configuration
- `OptelDataSourceInstrumentation` - Database instrumentation
- `OptelCacheInstrumentation` - Cache instrumentation
- `OptelHttpClientInstrumentation` - HTTP client instrumentation
- `OptelWebClientInstrumentation` - WebClient instrumentation
- `OptelGrpcInstrumentation` - gRPC instrumentation
- `OptelKafkaInstrumentation` - Kafka instrumentation
- `OptelInternalTracingAspect` - Internal method tracing

## Testing

Run tests:
```bash
./gradlew :cts-otel-spring-boot-adapter:test
```

## Dependencies

- cts-otel-core
- Spring Boot Starter
- Spring Boot Starter Web
- Spring Boot Starter Data JPA (optional)
- Spring Boot Starter Cache (optional)
- Spring Kafka (optional)
- gRPC Spring Boot Starter (optional)

## License

Copyright © 2025 CTS. All rights reserved.
