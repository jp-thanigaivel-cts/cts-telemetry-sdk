package com.example.demo.service.impl;

import com.example.demo.proto.HelloRequest;
import com.example.demo.proto.HelloServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

/**
 * Service responsible for gRPC client verification operations.
 * Tests gRPC client instrumentation for outbound gRPC calls.
 */
@Service
public class GrpcClientVerificationService {

    @GrpcClient("app2")
    private HelloServiceGrpc.HelloServiceBlockingStub helloServiceStub;

    /**
     * Verifies gRPC client instrumentation by calling App2's gRPC service.
     * Generates gRPC CLIENT span with trace context propagation via gRPC metadata.
     * 
     * @return Verification result message
     */
    public String verifyApp2Grpc() {
        try {
            HelloRequest request = HelloRequest.newBuilder().setName("App1").build();
            String message = helloServiceStub.sayHello(request).getMessage();
            return "App2 gRPC Check Result: Success (" + message + ")";
        } catch (Exception e) {
            return "App2 gRPC Check Result: Failed (" + e.getMessage() + ")";
        }
    }
}
