package com.cts.telemetry.api;

public enum HttpAttributes implements TelemetryAttribute {
    METHOD("http.request.method"),
    URL("url.full"),
    SCHEME("url.scheme"),
    STATUS_CODE("http.response.status_code"),
    ROUTE("http.route"),
    ERROR_TYPE("error.type"),
    NETWORK_PROTOCOL_NAME("network.protocol.name"),
    NETWORK_PROTOCOL_VERSION("network.protocol.version"),
    SERVER_ADDRESS("server.address"),
    SERVER_PORT("server.port"),
    USER_AGENT_SYNTHETIC_TYPE("user_agent.synthetic.type"),
    TRACE_ID("http.trace_id");

    private final String key;

    HttpAttributes(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
