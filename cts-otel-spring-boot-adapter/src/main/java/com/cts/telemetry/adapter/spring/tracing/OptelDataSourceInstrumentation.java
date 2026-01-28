package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.adapter.spring.metrics.DbMetricsHandler;
import com.cts.telemetry.api.DbAttributes;
import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.tracing.OptelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OptelDataSourceInstrumentation implements BeanPostProcessor {

    private final OptelConfig config;
    private final DbMetricsHandler metricsHandler;

    public OptelDataSourceInstrumentation(OptelConfig config) {
        this.config = config;
        this.metricsHandler = new DbMetricsHandler(config);
        // Ensure OpenTelemetry is initialized before we start creating proxies that use
        // it
        com.cts.telemetry.init.OptelInitializer.initialize(config);
        log.debug("OptelDataSourceInstrumentation initialized");
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            // Check for HikariCP and configure metrics tracker if enabled
            if (config.getMetrics().isEnabled()) {
                configureHikariMetrics(bean);
            }

            if (config.getTracing().isEnabled() &&
                    config.getTracing().getInstrument().getDb() != null &&
                    config.getTracing().getInstrument().getDb().isEnabled()) {
                return Proxy.newProxyInstance(
                        bean.getClass().getClassLoader(),
                        new Class[] { DataSource.class },
                        new DataSourceInvocationHandler((DataSource) bean, config, metricsHandler));
            }
        }
        return bean;
    }

    private void configureHikariMetrics(Object bean) {
        try {
            // Use reflection to check if it's a HikariDataSource to avoid direct class
            // references
            // that might fail during class loading if Hikari is not present
            Class<?> hikariDSClass = Class.forName("com.zaxxer.hikari.HikariDataSource");
            if (hikariDSClass.isInstance(bean)) {
                log.debug("HikariDataSource detected, configuring metrics tracker factory");

                // Get the factory class
                Class<?> factoryClass = Class
                        .forName("com.cts.telemetry.adapter.spring.metrics.hikari.OptelHikariMetricsTrackerFactory");
                Object factory = factoryClass.getConstructor(OptelConfig.class).newInstance(config);

                Method setMetricsTrackerFactoryMethod = hikariDSClass.getMethod("setMetricsTrackerFactory",
                        Class.forName("com.zaxxer.hikari.metrics.MetricsTrackerFactory"));

                setMetricsTrackerFactoryMethod.invoke(bean, factory);
            }
        } catch (ClassNotFoundException e) {
            // HikariCP or our factory not on classpath, ignore
            log.trace("HikariCP classes not found, skipping Hikari metrics configuration");
        } catch (Throwable t) {
            log.warn("Failed to configure Hikari metrics tracker via reflection (continuing without pool metrics): {}",
                    t.getMessage());
        }
    }

    private static class DataSourceInvocationHandler implements InvocationHandler {
        private final DataSource target;
        private final OptelConfig config;
        private final DbMetricsHandler metricsHandler;

        public DataSourceInvocationHandler(DataSource target, OptelConfig config, DbMetricsHandler metricsHandler) {
            this.target = target;
            this.config = config;
            this.metricsHandler = metricsHandler;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result;
            try {
                result = method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }

            if ("getConnection".equals(method.getName()) && result instanceof Connection) {
                return Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class[] { Connection.class },
                        new ConnectionInvocationHandler((Connection) result, config, metricsHandler));
            }
            return result;
        }
    }

    private static class ConnectionInvocationHandler implements InvocationHandler {
        private final Connection target;
        private final OptelConfig config;
        private final DbMetricsHandler metricsHandler;

        public ConnectionInvocationHandler(Connection target, OptelConfig config, DbMetricsHandler metricsHandler) {
            this.target = target;
            this.config = config;
            this.metricsHandler = metricsHandler;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result;
            try {
                result = method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }

            String methodName = method.getName();
            if (result instanceof Statement) {
                if ("createStatement".equals(methodName)) {
                    return createStatementProxy(result, null, Statement.class);
                } else if ("prepareStatement".equals(methodName) && args != null && args.length > 0
                        && args[0] instanceof String) {
                    return createStatementProxy(result, (String) args[0], PreparedStatement.class);
                } else if ("prepareCall".equals(methodName) && args != null && args.length > 0
                        && args[0] instanceof String) {
                    return createStatementProxy(result, (String) args[0], CallableStatement.class);
                }
            }
            return result;
        }

        private Object createStatementProxy(Object statement, String sql, Class<?> interfaceType) {
            // Ensure we include all interfaces implemented by the statement, or at least
            // the primary one + Wrapper + AutoCloseable
            // For simplicity, we use the specific JDBC interface requested.
            return Proxy.newProxyInstance(
                    statement.getClass().getClassLoader(),
                    new Class[] { interfaceType },
                    new StatementInvocationHandler(statement, sql, config, metricsHandler));
        }
    }

    private static class StatementInvocationHandler implements InvocationHandler {
        private final Object target;
        private final String preparedSql;
        private final OptelConfig config;
        private final DbMetricsHandler metricsHandler;

        public StatementInvocationHandler(Object target, String preparedSql, OptelConfig config,
                DbMetricsHandler metricsHandler) {
            this.target = target;
            this.preparedSql = preparedSql;
            this.config = config;
            this.metricsHandler = metricsHandler;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            boolean isExecution = methodName.startsWith("execute");

            if (isExecution) {
                String sql = preparedSql;
                if (sql == null && args != null && args.length > 0 && args[0] instanceof String) {
                    sql = (String) args[0];
                }

                String spanName = "DB " + methodName;
                long startTime = System.nanoTime();
                Span span = OptelTracer.startSpan(spanName, SpanKind.CLIENT);

                Map<String, String> attributes = new HashMap<>();
                attributes.put(DbAttributes.SYSTEM_NAME.key(), "sql");
                attributes.put(DbAttributes.OPERATION_NAME.key(), methodName);

                if (config.getTracing().getInstrument().getDb().isCaptureSql() && sql != null) {
                    attributes.put(DbAttributes.QUERY_TEXT.key(), sql);
                }

                try (Scope scope = span.makeCurrent()) {
                    attributes.forEach(span::setAttribute);
                    Object result = method.invoke(target, args);
                    return result;
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getTargetException();
                    span.recordException(cause);
                    span.setStatus(StatusCode.ERROR, cause.getMessage());
                    attributes.put(DbAttributes.ERROR_TYPE.key(), cause.getClass().getName());
                    throw cause;
                } catch (Throwable t) {
                    span.recordException(t);
                    span.setStatus(StatusCode.ERROR, t.getMessage());
                    attributes.put(DbAttributes.ERROR_TYPE.key(), t.getClass().getName());
                    throw t;
                } finally {
                    long endTime = System.nanoTime();
                    span.end(endTime, TimeUnit.NANOSECONDS);
                    double durationSeconds = (endTime - startTime) / 1_000_000_000.0;
                    metricsHandler.recordMetrics(attributes, durationSeconds);
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
