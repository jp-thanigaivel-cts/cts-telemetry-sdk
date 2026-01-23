package com.cts.telemetry.api.constants;

/**
 * Error codes for dependency validation failures.
 * These codes are used to identify specific missing dependencies when
 * instrumentation is enabled.
 */
public final class DependencyErrorCodes {

    private DependencyErrorCodes() {
        // Prevent instantiation
    }

    /**
     * Error code for missing DataSource dependency.
     * Thrown when DB instrumentation is enabled but javax.sql.DataSource is not on
     * the classpath.
     */
    public static final String MISSING_DATASOURCE_DEPENDENCY = "OPTEL-DEP-001";

    /**
     * Error code for missing gRPC dependency.
     * Thrown when gRPC instrumentation is enabled but io.grpc.ManagedChannel is not
     * on the classpath.
     */
    public static final String MISSING_GRPC_DEPENDENCY = "OPTEL-DEP-002";

    /**
     * Error code for missing Kafka dependency.
     * Thrown when Kafka instrumentation is enabled but Kafka client classes are not
     * on the classpath.
     */
    public static final String MISSING_KAFKA_DEPENDENCY = "OPTEL-DEP-003";

    /**
     * Error code for missing Cache dependency.
     * Thrown when Cache instrumentation is enabled but Spring Cache classes are not
     * on the classpath.
     */
    public static final String MISSING_CACHE_DEPENDENCY = "OPTEL-DEP-004";

    /**
     * Error code for missing HTTP client dependency.
     * Thrown when HTTP instrumentation is enabled but required HTTP client classes
     * are not on the classpath.
     */
    public static final String MISSING_HTTP_DEPENDENCY = "OPTEL-DEP-005";
}
