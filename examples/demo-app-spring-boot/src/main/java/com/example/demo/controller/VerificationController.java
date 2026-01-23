package com.example.demo.controller;

import com.example.demo.service.impl.CacheVerificationService;
import com.example.demo.service.impl.GrpcClientVerificationService;
import com.example.demo.service.impl.HttpClientVerificationService;
import com.example.demo.service.impl.InternalVerificationService;
import com.example.demo.service.base.VerificationService;
import com.example.demo.service.impl.WebClientVerificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller for telemetry verification endpoints.
 * Provides individual endpoints for testing specific telemetry types
 * and a comprehensive endpoint that tests all types.
 */
@RestController
@RequestMapping("/verify")
public class VerificationController {

    private final VerificationService verificationService;
    private final InternalVerificationService internalService;
    private final CacheVerificationService cacheService;
    private final HttpClientVerificationService httpClientService;
    private final WebClientVerificationService webClientService;
    private final GrpcClientVerificationService grpcClientService;

    public VerificationController(
            VerificationService verificationService,
            InternalVerificationService internalService,
            CacheVerificationService cacheService,
            HttpClientVerificationService httpClientService,
            WebClientVerificationService webClientService,
            GrpcClientVerificationService grpcClientService) {
        this.verificationService = verificationService;
        this.internalService = internalService;
        this.cacheService = cacheService;
        this.httpClientService = httpClientService;
        this.webClientService = webClientService;
        this.grpcClientService = grpcClientService;
    }

    @GetMapping("/all")
    public String verifyAll() {
        return verificationService.verifyAll();
    }

    @GetMapping("/db")
    public String verifyDb() {
        return cacheService.verifyDbRead();
    }

    @GetMapping("/db/insert")
    public String verifyDbInsert() {
        return internalService.verifyDbInsert();
    }

    @GetMapping("/http")
    public String verifyHttp() {
        return httpClientService.verifyExternalHttp();
    }

    @GetMapping("/webclient")
    public Mono<String> verifyWebClient() {
        return webClientService.verifyExternalWebClient();
    }

    @GetMapping("/internal")
    public String verifyInternal() {
        return internalService.internalBusinessLogic("test-data");
    }

    @GetMapping("/app2/http")
    public String verifyApp2Http() {
        return httpClientService.verifyApp2Http();
    }

    @GetMapping("/app2/grpc")
    public String verifyApp2Grpc() {
        return grpcClientService.verifyApp2Grpc();
    }
}
