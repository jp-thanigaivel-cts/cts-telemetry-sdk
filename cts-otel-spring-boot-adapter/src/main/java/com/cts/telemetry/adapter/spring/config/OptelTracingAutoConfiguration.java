package com.cts.telemetry.adapter.spring.config;

import com.cts.telemetry.adapter.spring.api.strategy.HttpServerStrategy;
import com.cts.telemetry.adapter.spring.api.strategy.KafkaConsumerStrategy;
import com.cts.telemetry.adapter.spring.tracing.*;
import com.cts.telemetry.config.OptelConfig;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "optel.tracing", name = "enabled", havingValue = "true")
public class OptelTracingAutoConfiguration {

    @Bean
    public OptelServerTracingInterceptor optelServerTracingInterceptor(OptelConfig config,
            Optional<HttpServerStrategy> strategy) {
        OptelServerTracingInterceptor interceptor = new OptelServerTracingInterceptor(config);
        strategy.ifPresent(interceptor::setStrategy);
        return interceptor;
    }

    @Bean
    public WebMvcConfigurer optelTracingWebMvcConfigurer(OptelServerTracingInterceptor interceptor) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor);
            }
        };
    }

    @Bean
    @ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
    public OptelInternalTracingAspect optelInternalTracingAspect(OptelConfig config) {
        return new OptelInternalTracingAspect(config);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.web.client.RestTemplate")
    public OptelHttpClientInstrumentation optelHttpClientInstrumentation(OptelConfig config) {
        return new OptelHttpClientInstrumentation(config);
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.web.reactive.function.client.WebClient")
    public OptelWebClientInstrumentation optelWebClientInstrumentation(OptelConfig config) {
        return new OptelWebClientInstrumentation(config);
    }

    @Bean
    @ConditionalOnClass(name = "javax.sql.DataSource")
    public OptelDataSourceInstrumentation optelDataSourceInstrumentation(OptelConfig config) {
        return new OptelDataSourceInstrumentation(config);
    }

    @Bean
    @ConditionalOnClass(name = "org.apache.kafka.clients.producer.ProducerInterceptor")
    public OptelKafkaProducerInterceptor optelKafkaProducerInterceptor(OptelConfig config) {
        return new OptelKafkaProducerInterceptor(config);
    }
    /*
     * @Bean
     * 
     * @ConditionalOnClass(name =
     * "org.apache.kafka.clients.consumer.ConsumerInterceptor")
     * public OptelKafkaConsumerInterceptor
     * optelKafkaConsumerInterceptor(OptelConfig config) {
     * return new OptelKafkaConsumerInterceptor();
     * }
     */

    @Bean
    @ConditionalOnClass(name = "org.apache.kafka.clients.consumer.ConsumerInterceptor")
    public OptelKafkaRecordInterceptor optelKafkaRecordInterceptor(OptelConfig config,
            Optional<KafkaConsumerStrategy> strategy) {
        OptelKafkaRecordInterceptor interceptor = new OptelKafkaRecordInterceptor(config);
        strategy.ifPresent(interceptor::setStrategy);
        return interceptor;
    }

    @Bean
    @ConditionalOnClass(name = "org.apache.kafka.clients.consumer.ConsumerInterceptor")
    public BeanPostProcessor kafkaLisentenerFactoryPostProcessor(
            OptelKafkaRecordInterceptor optelKafkaRecordInterceptor) {

        return new BeanPostProcessor() {
            @Override
            public @Nullable Object postProcessBeforeInitialization(Object bean, String beanName)
                    throws BeansException {
                log.info("Initializing bean: " + beanName);
                if (bean instanceof ConcurrentKafkaListenerContainerFactory) {
                    log.info("Configuring ConcurrentKafkaListenerContainerFactory with OptelKafkaRecordInterceptor");
                    ConcurrentKafkaListenerContainerFactory factory = (ConcurrentKafkaListenerContainerFactory<?, ?>) bean;
                    factory.setRecordInterceptor(optelKafkaRecordInterceptor);
                    log.info("OptelKafkaRecordInterceptor set on ConcurrentKafkaListenerContainerFactory");
                }
                return bean;
            }
        };
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.cache.CacheManager")
    @ConditionalOnProperty(prefix = "optel.tracing.instrument.cache", name = "enabled", havingValue = "true")
    public OptelCacheInstrumentation optelCacheInstrumentation(OptelConfig config) {
        return new OptelCacheInstrumentation(config);
    }
}
