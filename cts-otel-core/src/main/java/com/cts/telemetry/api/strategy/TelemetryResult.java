package com.cts.telemetry.api.strategy;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the result of an application-level telemetry logic check.
 */
@Data
@Builder
public class TelemetryResult {
    private String statusCode;
    private String statusDescription;
    private StatusType statusType;
    @Builder.Default
    private Map<String, String> attributes = new HashMap<>();
}
