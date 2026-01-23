# CTS OpenTelemetry Jersey Adapter

## Overview
The `cts-otel-jersey-adapter` module provides OpenTelemetry integration for Jersey (JAX-RS) applications with automatic instrumentation for HTTP requests and metrics collection.

## Features

- ✅ **HTTP Server Tracing** - Automatic SERVER span creation for Jersey endpoints
- ✅ **HTTP Metrics** - Request duration, count, and status code metrics
- ✅ **Trace Propagation** - W3C Trace Context propagation
- ✅ **Error Tracking** - Automatic exception recording
- ✅ **Easy Integration** - Simple feature registration

## Installation

### Gradle
```kotlin
dependencies {
    implementation("com.cts:cts-otel-jersey-adapter:1.0.0")
}
```

### Maven
```xml
<dependency>
    <groupId>com.cts</groupId>
    <artifactId>cts-otel-jersey-adapter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration

### Jersey Application Registration

```java
import com.cts.telemetry.adapter.jersey.OptelJerseyFeature;
import org.glassfish.jersey.server.ResourceConfig;

public class MyApplication extends ResourceConfig {
    public MyApplication() {
        // Register your resources
        packages("com.example.api");
        
        // Register OpenTelemetry feature
        register(OptelJerseyFeature.class);
    }
}
```

### Configuration Properties

```yaml
optel:
  enabled: true
  serviceName: my-jersey-app
  endpoint: http://localhost:4317
  exporter: BOTH
  
  metrics:
    enabled: true
  
  tracing:
    enabled: true
    instrument:
      http:
        enabled: true
```

## Usage

### Basic Jersey Resource

```java
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    
    @GET
    @Path("/{id}")
    public User getUser(@PathParam("id") Long id) {
        // Automatic SERVER span created
        // Automatic metrics recorded
        return userService.findById(id);
    }
    
    @POST
    public User createUser(User user) {
        // Automatic tracing and metrics
        return userService.create(user);
    }
}
```

### Span Attributes

The Jersey adapter automatically captures:

**HTTP Attributes:**
- `http.method` - HTTP method (GET, POST, etc.)
- `http.route` - Request path template
- `http.status_code` - Response status code
- `http.scheme` - HTTP or HTTPS
- `http.target` - Full request URI

**Custom Attributes:**
```java
import io.opentelemetry.api.trace.Span;

@Path("/orders")
public class OrderResource {
    
    @POST
    public Order createOrder(Order order) {
        Span currentSpan = Span.current();
        currentSpan.setAttribute("order.id", order.getId());
        currentSpan.setAttribute("order.total", order.getTotal());
        
        return orderService.create(order);
    }
}
```

## Filters

### OptelJerseyTracingFilter

Creates SERVER spans for incoming requests and propagates trace context.

**Features:**
- Automatic span creation
- Trace context extraction from headers
- Exception recording
- Span attribute population

### OptelJerseyMetricsFilter

Records HTTP metrics for requests.

**Metrics Collected:**
- `http.server.request.duration` - Request duration histogram
- Attributes: method, route, status_code

## Trace Propagation

The adapter automatically:
1. Extracts trace context from incoming `traceparent` header
2. Creates child spans for the request
3. Injects trace context into outbound calls (if using instrumented HTTP clients)

**Example Trace Flow:**
```
Client Request (traceparent: 00-trace-id-span-id-01)
    ↓
Jersey Server (extracts context)
    ↓
SERVER Span (parent: span-id)
    ↓
Business Logic
    ↓
Response (with trace context)
```

## Error Handling

Exceptions are automatically recorded in spans:

```java
@Path("/api")
public class ApiResource {
    
    @GET
    public Response getData() {
        try {
            return Response.ok(service.getData()).build();
        } catch (Exception e) {
            // Exception automatically recorded in span
            // Span status set to ERROR
            throw new WebApplicationException(e, 500);
        }
    }
}
```

## Integration with Other Components

### With Jersey Client

```java
import org.glassfish.jersey.client.ClientConfig;

ClientConfig config = new ClientConfig();
Client client = ClientBuilder.newClient(config);
// Note: For client-side tracing, use OptelHttpClientInstrumentation
```

### With Dependency Injection

```java
import javax.inject.Inject;

@Path("/users")
public class UserResource {
    
    @Inject
    private UserService userService;
    
    @GET
    public List<User> getUsers() {
        // Service calls can be traced with internal instrumentation
        return userService.findAll();
    }
}
```

## Testing

Run tests:
```bash
./gradlew :cts-otel-jersey-adapter:test
```

## Example Application

```java
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class JerseyApp {
    public static void main(String[] args) throws Exception {
        ResourceConfig config = new ResourceConfig();
        config.packages("com.example.api");
        config.register(OptelJerseyFeature.class);
        
        ServletHolder servlet = new ServletHolder(new ServletContainer(config));
        
        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(server, "/*");
        context.addServlet(servlet, "/*");
        
        server.start();
        server.join();
    }
}
```

## Dependencies

- cts-otel-core
- Jersey Server
- JAX-RS API
- Servlet API

## License

Copyright © 2025 CTS. All rights reserved.
