package com.cts.telemetry.api;

/**
 * OpenTelemetry Database Semantic Conventions.
 * 
 * @see <a href=
 *      "https://opentelemetry.io/docs/specs/semconv/database/database-spans/">Database
 *      Spans</a>
 */
public enum DbAttributes implements TelemetryAttribute {
    SYSTEM_NAME("db.system.name"),
    COLLECTION_NAME("db.collection.name"),
    NAMESPACE("db.namespace"),
    OPERATION_NAME("db.operation.name"),
    RESPONSE_STATUS_CODE("db.response.status_code"),
    QUERY_TEXT("db.query.text"),
    QUERY_SUMMARY("db.query.summary"),
    STORED_PROCEDURE_NAME("db.stored_procedure.name"),
    SERVER_ADDRESS("server.address"),
    SERVER_PORT("server.port"),
    NETWORK_PEER_ADDRESS("network.peer.address"),
    NETWORK_PEER_PORT("network.peer.port"),
    ERROR_TYPE("error.type");

    private final String key;

    DbAttributes(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
