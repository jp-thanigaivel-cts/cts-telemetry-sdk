package com.example.demo.service.impl;

import com.example.demo.entity.VerificationEntity;
import com.example.demo.repository.VerificationRepository;
import org.springframework.stereotype.Service;

/**
 * Service responsible for internal business logic and database operations
 * verification.
 * Tests internal span instrumentation and database operations.
 */
@Service
public class InternalVerificationService {

    private final VerificationRepository repository;

    public InternalVerificationService(VerificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Verifies database insert operation.
     * Generates DB CLIENT span for the insert operation.
     * 
     * @return Verification result message
     */
    public String verifyDbInsert() {
        VerificationEntity obj = new VerificationEntity();
        obj.setMessage("message");
        obj = repository.save(obj);
        return "DB Check Insert: Id=" + obj.getId();
    }

    /**
     * Internal business logic method to generate INTERNAL spans.
     * This method will be instrumented by the internal tracing configuration.
     * 
     * @param input Input data to process
     * @return Verification result message
     */
    public String internalBusinessLogic(String input) {
        // Simulate some business logic processing
        String processed = processData(input);
        String validated = validateData(processed);
        return "Internal processing completed: " + validated;
    }

    /**
     * Helper method for internal span generation.
     * Simulates data processing logic.
     * 
     * @param data Input data
     * @return Processed data
     */
    private String processData(String data) {
        // Simulate data processing
        try {
            Thread.sleep(10); // Small delay to make span visible
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return data.toUpperCase();
    }

    /**
     * Helper method for internal span generation.
     * Simulates data validation logic.
     * 
     * @param data Data to validate
     * @return Validated data
     */
    private String validateData(String data) {
        // Simulate data validation
        try {
            Thread.sleep(10); // Small delay to make span visible
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return data + "-VALIDATED";
    }
}
