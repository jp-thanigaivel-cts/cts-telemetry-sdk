package com.cts.telemetry.api.constants;

/**
 * Error messages for dependency validation failures.
 * These messages provide detailed information about missing dependencies.
 */
public final class DependencyErrorMessages {

        private DependencyErrorMessages() {
                // Prevent instantiation
        }

        /**
         * Error message for missing DataSource dependency.
         */
        public static final String MISSING_DATASOURCE_DEPENDENCY = "Optel DB instrumentation is enabled but 'javax.sql.DataSource' is missing from the classpath. "
                        +
                        "Please add the required JDBC dependency to your project.";

        /**
         * Error message for missing gRPC dependency.
         */
        public static final String MISSING_GRPC_DEPENDENCY = "Optel gRPC instrumentation is enabled but 'io.grpc.ManagedChannel' is missing from the classpath. "
                        +
                        "Please add the required gRPC dependency to your project.";

        /**
         * Error message for missing Kafka dependency.
         */
        public static final String MISSING_KAFKA_DEPENDENCY = "Optel Kafka instrumentation is enabled but 'org.apache.kafka.clients.producer.ProducerInterceptor' is missing from the classpath. "
                        +
                        "Please add the required Kafka client dependency to your project.";

        /**
         * Error message for missing Cache dependency.
         */
        public static final String MISSING_CACHE_DEPENDENCY = "Optel Cache instrumentation is enabled but 'org.springframework.cache.CacheManager' is missing from the classpath. "
                        +
                        "Please add the required Spring Cache dependency to your project.";

        public static final String MISSING_HTTP_DEPENDENCY = "Optel HTTP instrumentation is enabled but required HTTP client classes are missing from the classpath. "
                        +
                        "Please add the required HTTP client dependency to your project.";

        /**
         * Error message for missing JMS dependency.
         */
        public static final String MISSING_JMS_DEPENDENCY = "Optel JMS instrumentation is enabled but 'jakarta.jms.Message' is missing from the classpath. "
                        +
                        "Please add the required JMS dependency to your project.";

        /**
         * Error message for missing AspectJ dependency.
         */
        public static final String MISSING_ASPECTJ_DEPENDENCY = "Optel Internal instrumentation is enabled but 'org.aspectj.lang.annotation.Aspect' is missing from the classpath. "
                        +
                        "Please add the required AspectJ dependency to your project.";
}
