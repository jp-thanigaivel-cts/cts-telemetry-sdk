package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.tracing.OptelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.Callable;

@Slf4j
public class OptelCacheInstrumentation implements BeanPostProcessor {

    private final OptelConfig config;

    public OptelCacheInstrumentation(OptelConfig config) {
        this.config = config;
        com.cts.telemetry.init.OptelInitializer.initialize(config);
        log.debug("OptelCacheInstrumentation initialized");
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof CacheManager) {
            if (config.getTracing().isEnabled() &&
                    config.getTracing().getInstrument().getCache() != null &&
                    config.getTracing().getInstrument().getCache().isEnabled()) {
                return Proxy.newProxyInstance(
                        bean.getClass().getClassLoader(),
                        bean.getClass().getInterfaces(),
                        new CacheManagerInvocationHandler((CacheManager) bean, config));
            }
        }
        return bean;
    }

    private static class CacheManagerInvocationHandler implements InvocationHandler {
        private final CacheManager target;
        private final OptelConfig config;

        public CacheManagerInvocationHandler(CacheManager target, OptelConfig config) {
            this.target = target;
            this.config = config;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result;
            try {
                result = method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }

            if ("getCache".equals(method.getName()) && result instanceof Cache) {
                return Proxy.newProxyInstance(
                        Cache.class.getClassLoader(),
                        new Class[] { Cache.class },
                        new CacheInvocationHandler((Cache) result, config));
            }
            return result;
        }
    }

    private static class CacheInvocationHandler implements InvocationHandler {
        private final Cache target;
        private final OptelConfig config;

        public CacheInvocationHandler(Cache target, OptelConfig config) {
            this.target = target;
            this.config = config;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            boolean isCacheOperation = methodName.equals("get") || methodName.equals("put") ||
                    methodName.equals("evict") || methodName.equals("clear") ||
                    methodName.equals("putIfAbsent");

            if (isCacheOperation) {
                String cacheName = target.getName();
                String spanName = "Cache " + methodName + " " + cacheName;
                Span span = OptelTracer.startSpan(spanName, SpanKind.CLIENT);

                try (Scope scope = span.makeCurrent()) {
                    span.setAttribute(com.cts.telemetry.api.SpanAttributes.CACHE_SYSTEM.key(), "spring-cache");
                    span.setAttribute(com.cts.telemetry.api.SpanAttributes.CACHE_OPERATION.key(), methodName);
                    span.setAttribute(com.cts.telemetry.api.SpanAttributes.CACHE_NAME.key(), cacheName);

                    if (args != null && args.length > 0) {
                        span.setAttribute(com.cts.telemetry.api.SpanAttributes.CACHE_KEY.key(),
                                String.valueOf(args[0]));
                    }

                    Object result = method.invoke(target, args);

                    // For get operations, record hit/miss
                    if ("get".equals(methodName)) {
                        if (args.length == 1) {
                            // get(Object key)
                            Cache.ValueWrapper valueWrapper = (Cache.ValueWrapper) result;
                            span.setAttribute(com.cts.telemetry.api.SpanAttributes.CACHE_HIT.key(),
                                    valueWrapper != null && valueWrapper.get() != null);
                        } else if (args.length == 2 && args[1] instanceof Callable) {
                            // get(Object key, Callable<T> valueLoader)
                            span.setAttribute(com.cts.telemetry.api.SpanAttributes.CACHE_HIT.key(), result != null);
                        }
                    }

                    return result;
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getTargetException();
                    span.recordException(cause);
                    span.setStatus(StatusCode.ERROR, cause.getMessage());
                    throw cause;
                } catch (Throwable t) {
                    span.recordException(t);
                    span.setStatus(StatusCode.ERROR, t.getMessage());
                    throw t;
                } finally {
                    span.end();
                }
            } else {
                try {
                    return method.invoke(target, args);
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            }
        }
    }
}
