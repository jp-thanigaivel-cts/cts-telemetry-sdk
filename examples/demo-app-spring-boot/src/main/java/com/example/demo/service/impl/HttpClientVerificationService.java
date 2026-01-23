package com.example.demo.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service responsible for HTTP client verification operations using
 * RestTemplate.
 * Tests HTTP client instrumentation for outbound HTTP calls.
 */
@Service
public class HttpClientVerificationService {

    private final RestTemplate restTemplate;

    public HttpClientVerificationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Verifies HTTP client instrumentation by calling an external public API.
     * Generates HTTP CLIENT span with traceparent header injection.
     * 
     * @return Verification result message
     */
    public String verifyExternalHttp() {
        String url = "https://rapidapi.com/otha1920/api/bullbear-advisor/stock/quote";
        try {
            String response = restTemplate.getForObject(url, String.class);
            return "HTTP Check Result: Success (Length: " + (response != null ? response.length() : 0) + ")";
        } catch (Exception e) {
            return "HTTP Check Result: Failed (" + e.getMessage() + ")";
        }
    }

    /**
     * Verifies cross-service HTTP communication by calling App2's HTTP endpoint.
     * Tests trace context propagation between services.
     * 
     * @return Verification result message
     */
    public String verifyApp2Http() {
        String url = "http://localhost:8081/hello";
        try {
            String response = restTemplate.getForObject(url, String.class);
            return "App2 HTTP Check Result: Success (" + response + ")";
        } catch (Exception e) {
            return "App2 HTTP Check Result: Failed (" + e.getMessage() + ")";
        }
    }
}
