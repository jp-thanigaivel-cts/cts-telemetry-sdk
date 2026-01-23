package com.example.demo.service.base;

import com.example.demo.service.impl.*;
import org.springframework.stereotype.Service;

/**
 * Main verification service that orchestrates comprehensive telemetry testing.
 * Delegates to specialized services for different client types and operations.
 */
@Service
public class VerificationService {

    private final InternalVerificationService internalService;
    private final CacheVerificationService cacheService;
    private final HttpClientVerificationService httpClientService;
    private final WebClientVerificationService webClientService;
    private final GrpcClientVerificationService grpcClientService;

    public VerificationService(
            InternalVerificationService internalService,
            CacheVerificationService cacheService,
            HttpClientVerificationService httpClientService,
            WebClientVerificationService webClientService,
            GrpcClientVerificationService grpcClientService) {
        this.internalService = internalService;
        this.cacheService = cacheService;
        this.httpClientService = httpClientService;
        this.webClientService = webClientService;
        this.grpcClientService = grpcClientService;
    }

    /**
     * Comprehensive verification endpoint that tests ALL telemetry features in a
     * single API call:
     * - SERVER span (incoming HTTP request to this endpoint)
     * - DB spans (insert and read operations)
     * - CACHE spans (cache population and hit)
     * - HTTP CLIENT spans (outbound HTTP via RestTemplate)
     * - WebClient spans (outbound HTTP via WebClient)
     * - gRPC CLIENT spans (outbound gRPC call to App2)
     * - HTTP CLIENT spans to App2 (cross-service HTTP)
     * - INTERNAL spans (internal business logic methods)
     * - Metrics (JVM, HTTP, DB, Cache metrics automatically collected)
     */
    public String verifyAll() {
        StringBuilder result = new StringBuilder("=== COMPREHENSIVE TELEMETRY VERIFICATION ===\n\n");

        // 1. DB Operations (generates DB spans + DB metrics)
        result.append("1. DATABASE OPERATIONS:\n");
        String dbInsert = internalService.verifyDbInsert();
        result.append("   - Insert: ").append(dbInsert).append("\n");
        String dbRead = cacheService.verifyDbRead();
        result.append("   - Read: ").append(dbRead).append("\n\n");

        // 2. Cache Operations (generates CACHE spans + Cache metrics)
        result.append("2. CACHE OPERATIONS:\n");
        String cacheResult = cacheService.verifyCacheHit();
        result.append("   - ").append(cacheResult).append("\n\n");

        // 3. Internal Spans (generates INTERNAL spans)
        result.append("3. INTERNAL BUSINESS LOGIC:\n");
        String internalResult = internalService.internalBusinessLogic("test-data");
        result.append("   - ").append(internalResult).append("\n\n");

        // 4. HTTP Client Spans (generates HTTP CLIENT spans via RestTemplate)
        result.append("4. HTTP CLIENT (RestTemplate):\n");
        String httpResult = httpClientService.verifyExternalHttp();
        result.append("   - ").append(httpResult).append("\n\n");

        // 5. WebClient Spans (generates HTTP CLIENT spans via WebClient)
        result.append("5. WEBCLIENT (Reactive HTTP):\n");
        String webClientResult = webClientService.verifyExternalWebClient().block();
        result.append("   - ").append(webClientResult).append("\n\n");

        // 6. Cross-Service HTTP (generates HTTP CLIENT span to App2)
        result.append("6. CROSS-SERVICE HTTP (App1 -> App2):\n");
        String app2HttpResult = httpClientService.verifyApp2Http();
        result.append("   - ").append(app2HttpResult).append("\n\n");

        // 7. gRPC Client Spans (generates gRPC CLIENT span to App2)
        result.append("7. GRPC CLIENT (App1 -> App2):\n");
        String app2GrpcResult = grpcClientService.verifyApp2Grpc();
        result.append("   - ").append(app2GrpcResult).append("\n\n");

        result.append("=== VERIFICATION COMPLETE ===\n");
        result.append(
                "All telemetry types triggered: SERVER, DB, CACHE, HTTP CLIENT, WebClient, gRPC CLIENT, INTERNAL\n");
        result.append("Metrics collected: JVM, HTTP, DB, Cache\n");

        return result.toString();
    }
}
