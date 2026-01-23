package com.cts.telemetry.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;

public class TraceUtils {

    public static <C> void inject(Context context, C carrier,
            io.opentelemetry.context.propagation.TextMapSetter<C> setter) {
        GlobalOpenTelemetry.getPropagators().getTextMapPropagator().inject(context, carrier, setter);
    }

    public static <C> Context extract(Context context, C carrier,
            io.opentelemetry.context.propagation.TextMapGetter<C> getter) {
        return GlobalOpenTelemetry.getPropagators().getTextMapPropagator().extract(context, carrier, getter);
    }
}
