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
        validateJmsDependency();
        validateInternalDependency();
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
        if (config.getTracing().getInstrument().getKafka().isProducer()) {
            if (!ClassUtils.isPresent("org.apache.kafka.clients.producer.ProducerInterceptor", null)) {
                throw new OptelDependencyException(
                        DependencyErrorCodes.MISSING_KAFKA_DEPENDENCY,
                        DependencyErrorMessages.MISSING_KAFKA_DEPENDENCY);
            }
        }
        if (config.getTracing().getInstrument().getKafka().isConsumer()) {
            if (!ClassUtils.isPresent("org.apache.kafka.clients.consumer.ConsumerInterceptor", null)) {
                throw new OptelDependencyException(
                        DependencyErrorCodes.MISSING_KAFKA_DEPENDENCY,
                        "Optel Kafka Consumer instrumentation is enabled but 'org.apache.kafka.clients.consumer.ConsumerInterceptor' is missing from the classpath.");
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
            // For HTTP server tracing
            if (!ClassUtils.isPresent("jakarta.servlet.http.HttpServletRequest", null)) {
                throw new OptelDependencyException(
                        DependencyErrorCodes.MISSING_HTTP_DEPENDENCY,
                        "Optel HTTP instrumentation is enabled but 'jakarta.servlet.http.HttpServletRequest' is missing from the classpath.");
            }
        }
    }

    private void validateJmsDependency() {
        if (config.getTracing().getInstrument().getJms().isEnabled()) {
            if (!ClassUtils.isPresent("jakarta.jms.Message", null)) {
                throw new OptelDependencyException(
                        DependencyErrorCodes.MISSING_JMS_DEPENDENCY,
                        DependencyErrorMessages.MISSING_JMS_DEPENDENCY);
            }
        }
    }

    private void validateInternalDependency() {
        if (config.getTracing().getInstrument().getInternal().isEnabled()) {
            if (!ClassUtils.isPresent("org.aspectj.lang.annotation.Aspect", null)) {
                throw new OptelDependencyException(
                        DependencyErrorCodes.MISSING_ASPECTJ_DEPENDENCY,
                        DependencyErrorMessages.MISSING_ASPECTJ_DEPENDENCY);
            }
        }
    }
}
