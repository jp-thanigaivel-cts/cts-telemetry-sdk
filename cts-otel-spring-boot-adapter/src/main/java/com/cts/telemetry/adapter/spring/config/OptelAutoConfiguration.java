package com.cts.telemetry.adapter.spring.config;

import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.init.OptelInitializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "optel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OptelAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "optel")
    public OptelConfig optelConfig() {
        return new OptelConfig();
    }

    @Bean
    public OptelInitializer optelInitializer(OptelConfig config) {
        OptelInitializer.initialize(config);
        return new OptelInitializer();
    }

    @Bean(destroyMethod = "stop")
    public com.cts.telemetry.metrics.MemoryMetricsPublisher memoryMetricsPublisher(OptelConfig config,
            OptelInitializer initializer) {
        com.cts.telemetry.metrics.MemoryMetricsPublisher publisher = new com.cts.telemetry.metrics.MemoryMetricsPublisher();
        publisher.start(config);
        return publisher;
    }

    @Bean
    public OptelDependencyValidator optelDependencyValidator(OptelConfig config) {
        return new OptelDependencyValidator(config);
    }
}
