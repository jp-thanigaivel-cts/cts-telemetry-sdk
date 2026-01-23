# Demo App - Spring Cache Instrumentation

This demo application demonstrates automatic cache instrumentation using the CTS Telemetry SDK.

## Features

- **Automatic Cache Instrumentation**: All Spring Cache operations are automatically traced
- **No Code Changes Required**: Just add `@Cacheable`, `@CacheEvict`, etc. annotations
- **Configurable**: Enable/disable cache tracing via configuration
- **Telemetry Export**: Cache spans are exported to OpenTelemetry Collector via `cts-otel-core`

## How It Works

1. **Application Layer** (`demo-app-spring-boot`):
   - Uses Spring Cache with `@Cacheable` annotations
   - No telemetry-specific code required

2. **Adapter Layer** (`cts-otel-spring-boot-adapter`):
   - `OptelCacheInstrumentation` automatically detects `CacheManager` beans
   - Wraps cache operations with tracing using Java Proxy pattern
   - Records cache hits/misses, operation types, cache names, and keys

3. **Core Layer** (`cts-otel-core`):
   - Exports traces to OpenTelemetry Collector
   - No caching-specific logic - pure OpenTelemetry

## Configuration

Enable cache instrumentation in `application.yml`:

```yaml
optel:
  tracing:
    enabled: true
    instrument:
      cache:
        enabled: true  # Enable cache instrumentation
```

## Cache Spans

Each cache operation creates a span with the following attributes:

- `cache.system`: "spring-cache"
- `cache.operation`: get, put, evict, clear, putIfAbsent
- `cache.name`: Name of the cache
- `cache.key`: Cache key
- `cache.hit`: true/false (for get operations)

## Example Usage

```java
@Service
public class VerificationService {
    
    @Cacheable(value = "dbCount", key = "'count'")
    public String verifyDbRead() {
        long count = repository.count();
        return "DB Check Result: Count=" + count;
    }
    
    @CacheEvict(value = "dbCount", allEntries = true)
    public String clearCache() {
        return "Cache cleared";
    }
}
```

## Running the Demo

1. **Build the project**:
   ```bash
   ./gradlew :examples:demo-app-spring-boot:build
   ```

2. **Run the application**:
   ```bash
   ./gradlew :examples:demo-app-spring-boot:bootRun
   ```

3. **Test cache operations**:
   ```bash
   # This will trigger cache operations and show cache hits/misses
   curl http://localhost:8080/api/verification/all
   ```

4. **View traces**:
   - Check console output (LOGGING exporter is enabled by default)
   - Or configure GRPC exporter to send to OpenTelemetry Collector

## Trace Output Example

```
Cache get dbCount
  cache.system: spring-cache
  cache.operation: get
  cache.name: dbCount
  cache.key: count
  cache.hit: false

Cache get dbCount
  cache.system: spring-cache
  cache.operation: get
  cache.name: dbCount
  cache.key: count
  cache.hit: true  # Second call hits the cache!
```

## Architecture

```
┌─────────────────────────────────┐
│   Demo App (Spring Boot)       │
│   - @Cacheable methods          │
│   - Spring CacheManager         │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│   cts-otel-spring-boot-adapter  │
│   - OptelCacheInstrumentation   │
│   - Auto-detects CacheManager   │
│   - Wraps with Proxy            │
│   - Creates spans               │
└────────────┬────────────────────┘
             │
             ▼
┌─────────────────────────────────┐
│   cts-otel-core                 │
│   - OpenTelemetry SDK           │
│   - Exports to Collector        │
│   - NO cache logic              │
└─────────────────────────────────┘
```

## Notes

- Cache instrumentation uses the same BeanPostProcessor pattern as DataSource instrumentation
- Works with any Spring Cache implementation (Simple, Caffeine, Redis, etc.)
- Zero performance overhead when cache instrumentation is disabled
- Fully compatible with Spring Boot's auto-configuration
