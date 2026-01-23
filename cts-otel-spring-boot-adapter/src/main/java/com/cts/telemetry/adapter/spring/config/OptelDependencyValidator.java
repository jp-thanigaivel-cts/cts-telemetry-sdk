package com.cts.telemetry.adapter.spring.config;

import com.cts.telemetry.api.constants.DependencyErrorCodes;
import com.cts.telemetry.api.constants.DependencyErrorMessages;
import com.cts.telemetry.api.exception.OptelDependencyException;
import com.cts.telemetry.config.OptelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;

@Slf4j
public class OptelDependencyValidator implements InitializingBean {

    private final OptelConfig config;

    public OptelDependencyValidator(OptelConfig config) {
        this.config = config;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (config == null || config.getTracing() == null || !config.getTracing().isEnabled()) {
            return;
        }

        validateDbDependency();
        validateGrpcDependency();
        validateHttpDependency();
        validateKafkaDependency();
        validateCacheDependency();
    }

    private void validateDbDependency() {
        if (config.getTracing().getInstrument().getDb().isEnabled()) {
            if (!ClassUtils.isPresent("javax.sql.DataSource", null)) {
                throw new OptelDependencyException(
                        DependencyErrorCodes.MISSING_DATASOURCE_DEPENDENCY,
                        DependencyErrorMessages.MISSING_DATASOURCE_DEPENDENCY);
            }
        }
    }

    private void validateGrpcDependency() {
        if (config.getTracing().getInstrument().getGrpc().isEnabled()) {
            if (!ClassUtils.isPresent("io.grpc.ManagedChannel", null)) {
                throw new OptelDependencyException(
                        DependencyErrorCodes.MISSING_GRPC_DEPENDENCY,
                        DependencyErrorMessages.MISSING_GRPC_DEPENDENCY);
            }
        }
    }

    private void validateKafkaDependency() {
        if (config.getTracing().getInstrument().getKafka().isProducer()
                || config.getTracing().getInstrument().getKafka().isConsumer()) {
            if (!ClassUtils.isPresent("org.apache.kafka.clients.producer.ProducerInterceptor", null)) {
                throw new OptelDependencyException(
                        DependencyErrorCodes.MISSING_KAFKA_DEPENDENCY,
                        DependencyErrorMessages.MISSING_KAFKA_DEPENDENCY);
            }
        }
    }

    private void validateCacheDependency() {
        if (config.getTracing().getInstrument().getCache().isEnabled()) {
            if (!ClassUtils.isPresent("org.springframework.cache.CacheManager", null)) {
                throw new OptelDependencyException(
                        DependencyErrorCodes.MISSING_CACHE_DEPENDENCY,
                        DependencyErrorMessages.MISSING_CACHE_DEPENDENCY);
            }
        }
    }

    private void validateHttpDependency() {
        if (config.getTracing().getInstrument().getHttp().isEnabled()) {
            // HTTP instrumentation covers Server, RestTemplate, and WebClient.
            // Since 'http.enabled' is a broad flag, we cannot enforce the presence of a
            // specific client
            // without potentially breaking applications that only use Server tracing or a
            // different client.
            // Therefore, we skip strict dependency validation for HTTP for now,
            // relying on @ConditionalOnClass in the auto-configuration.
        }
    }
}
