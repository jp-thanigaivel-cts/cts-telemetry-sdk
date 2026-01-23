package com.example.demo.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Service responsible for reactive HTTP client verification operations using
 * WebClient.
 * Tests WebClient instrumentation for reactive outbound HTTP calls.
 */
@Service
public class WebClientVerificationService {

    private final WebClient webClient;

    public WebClientVerificationService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Verifies WebClient instrumentation by making a reactive HTTP call to an
     * external API.
     * Generates HTTP CLIENT span with traceparent header injection.
     * 
     * @return Mono containing verification result message
     */
    public Mono<String> verifyExternalWebClient() {
        String url = "https://rapidapi.com/otha1920/api/bullbear-advisor/stock/quote";
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> "WebClient Check Result: Success (Length: " + response.length() + ")")
                .onErrorResume(e -> Mono.just("WebClient Check Result: Failed (" + e.getMessage() + ")"));
    }

    /**
     * Verifies cross-service reactive HTTP communication by calling App2's HTTP
     * endpoint.
     * Tests trace context propagation in reactive flows.
     * 
     * @return Mono containing verification result message
     */
    public Mono<String> verifyApp2WebClient() {
        String url = "http://localhost:8081/hello";
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> "App2 WebClient Check Result: Success (" + response + ")")
                .onErrorResume(e -> Mono.just("App2 WebClient Check Result: Failed (" + e.getMessage() + ")"));
    }
}
