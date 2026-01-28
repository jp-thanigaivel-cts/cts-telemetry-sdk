package com.cts.telemetry.api;

public sealed interface TelemetryAttribute
        permits HttpAttributes, JvmAttributes, AppAttributes, SpanAttributes, MessagingAttributes, DbAttributes {
    String key();
}
