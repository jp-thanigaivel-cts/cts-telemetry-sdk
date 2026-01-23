package com.cts.telemetry.api;

public enum SpanAttributes implements TelemetryAttribute {
    // DB Attributes
    DB_SYSTEM_NAME("db.system.name"),
    DB_OPERATION_NAME("db.operation.name"),
    DB_QUERY_TEXT("db.query.text"),

    // Messaging Attributes
    MESSAGING_SYSTEM("messaging.system"),
    MESSAGING_DESTINATION("messaging.destination"),
    MESSAGING_KAFKA_MESSAGE_KEY("messaging.kafka.message.key"),
    MESSAGING_KAFKA_PARTITION("messaging.kafka.partition"),
    MESSAGING_KAFKA_OFFSET("messaging.kafka.offset"),
    MESSAGING_KAFKA_CONSUMER_GROUP_NAME("messaging.consumer.group.name"),
    MESSAGING_OPERATION_NAME("messaging.operation.name"),
    MESSAGING_OPERATION_TYPE("messaging.operation.type"),



    CACHE_SYSTEM("cache.system"),
    CACHE_OPERATION("cache.operation"),
    CACHE_NAME("cache.name"),
    CACHE_KEY("cache.key"),
    CACHE_HIT("cache.hit");

    private final String key;

    SpanAttributes(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
