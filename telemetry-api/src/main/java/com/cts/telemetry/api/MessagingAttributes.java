package com.cts.telemetry.api;

/**
 * Messaging attributes following OpenTelemetry Semantic Conventions v1.39.0.
 */
public enum MessagingAttributes implements TelemetryAttribute {
    SYSTEM("messaging.system"),
    OPERATION_NAME("messaging.operation.name"),
    OPERATION_TYPE("messaging.operation.type"),
    DESTINATION_NAME("messaging.destination.name"),
    CONSUMER_GROUP_NAME("messaging.consumer.group.name"),
    DESTINATION_PARTITION_ID("messaging.destination.partition.id"),
    ERROR_TYPE("error.type"),
    SERVER_ADDRESS("server.address"),
    SERVER_PORT("server.port"),
    CLIENT_ID("messaging.client.id");

    private final String key;

    MessagingAttributes(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
