package com.cts.telemetry.api;

public enum AppAttributes implements TelemetryAttribute {
    SERVICE_NAME("service.name"),
    SERVICE_VERSION("service.version"),
    ENVIRONMENT("service.environment"),
    TENANT_ID("tenant.id"),
    API_NAME("api.name"),
    ERROR_TYPE("error.type"),
    ERROR_MESSAGE("error.message"),
    TELEMETRY_SDK_LANGUAGE("telemetry.sdk.language"),
    TELEMETRY_SDK_NAME("telemetry.sdk.name"),
    TELEMETRY_SDK_VERSION("telemetry.sdk.version");

    private final String key;

    AppAttributes(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
