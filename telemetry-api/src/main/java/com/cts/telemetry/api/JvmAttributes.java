package com.cts.telemetry.api;

public enum JvmAttributes implements TelemetryAttribute {
    MEMORY_USED("jvm.memory.used"),
    MEMORY_COMMITTED("jvm.memory.committed"),
    MEMORY_MAX("jvm.memory.max"),
    GC_COUNT("jvm.gc.count"),
    GC_TIME("jvm.gc.time"),
    THREAD_COUNT("jvm.thread.count");

    private final String key;

    JvmAttributes(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }
}
