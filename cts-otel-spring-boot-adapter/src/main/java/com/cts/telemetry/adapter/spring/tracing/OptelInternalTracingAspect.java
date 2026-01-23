package com.cts.telemetry.adapter.spring.tracing;

import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.tracing.OptelTracer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.util.List;

@Slf4j
@Aspect
public class OptelInternalTracingAspect {

    private final OptelConfig config;

    public OptelInternalTracingAspect(OptelConfig config) {
        this.config = config;
    }

    @Around("execution(* com.example..*(..))")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!config.getTracing().isEnabled() ||
                config.getTracing().getInstrument().getInternal() == null ||
                config.getTracing().getInstrument().getInternal().getPackages() == null ||
                config.getTracing().getInstrument().getInternal().getPackages().length == 0) {
            return joinPoint.proceed();
        }

        // Check if the method's class is in one of the configured packages
        String className = joinPoint.getTarget().getClass().getName();
        String[] packages = config.getTracing().getInstrument().getInternal().getPackages();
        boolean shouldTrace = false;
        for (String pkg : packages) {
            if (className.startsWith(pkg)) {
                shouldTrace = true;
                break;
            }
        }
        if (!shouldTrace) {
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String spanName = signature.getDeclaringType().getSimpleName() + "." + signature.getName();

        Span span = OptelTracer.startSpan(spanName, SpanKind.INTERNAL);

        long startTime = System.currentTimeMillis();

        try (Scope scope = span.makeCurrent()) {
            span.setAttribute("code.namespace", signature.getDeclaringType().getName());
            span.setAttribute("code.function", signature.getName());

            Object result = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            span.setAttribute("execution.duration.ms", duration);

            return result;
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
            throw t;
        } finally {
            span.end();
        }
    }
}
