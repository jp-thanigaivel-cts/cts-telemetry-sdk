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

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

@Slf4j
public class OptelDataSourceInstrumentation implements BeanPostProcessor {

    private final OptelConfig config;

    public OptelDataSourceInstrumentation(OptelConfig config) {
        this.config = config;
        // Ensure OpenTelemetry is initialized before we start creating proxies that use
        // it
        com.cts.telemetry.init.OptelInitializer.initialize(config);
        log.debug("OptelDataSourceInstrumentation initialized");
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            if (config.getTracing().isEnabled() &&
                    config.getTracing().getInstrument().getDb() != null &&
                    config.getTracing().getInstrument().getDb().isEnabled()) {
                return Proxy.newProxyInstance(
                        bean.getClass().getClassLoader(),
                        new Class[] { DataSource.class },
                        new DataSourceInvocationHandler((DataSource) bean, config));
            }
        }
        return bean;
    }

    private static class DataSourceInvocationHandler implements InvocationHandler {
        private final DataSource target;
        private final OptelConfig config;

        public DataSourceInvocationHandler(DataSource target, OptelConfig config) {
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

            if ("getConnection".equals(method.getName()) && result instanceof Connection) {
                return Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class[] { Connection.class },
                        new ConnectionInvocationHandler((Connection) result, config));
            }
            return result;
        }
    }

    private static class ConnectionInvocationHandler implements InvocationHandler {
        private final Connection target;
        private final OptelConfig config;

        public ConnectionInvocationHandler(Connection target, OptelConfig config) {
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
                    new StatementInvocationHandler(statement, sql, config));
        }
    }

    private static class StatementInvocationHandler implements InvocationHandler {
        private final Object target;
        private final String preparedSql;
        private final OptelConfig config;

        public StatementInvocationHandler(Object target, String preparedSql, OptelConfig config) {
            this.target = target;
            this.preparedSql = preparedSql;
            this.config = config;
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
                Span span = OptelTracer.startSpan(spanName, SpanKind.CLIENT);

                try (Scope scope = span.makeCurrent()) {
                    span.setAttribute(com.cts.telemetry.api.SpanAttributes.DB_SYSTEM_NAME.key(), "sql");
                    span.setAttribute(com.cts.telemetry.api.SpanAttributes.DB_OPERATION_NAME.key(), methodName);
                    if (config.getTracing().getInstrument().getDb().isCaptureSql() && sql != null) {
                        span.setAttribute(com.cts.telemetry.api.SpanAttributes.DB_QUERY_TEXT.key(), sql);
                    }

                    Object result = method.invoke(target, args);
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
