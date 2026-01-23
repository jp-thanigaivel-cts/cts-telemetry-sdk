package com.cts.telemetry.metrics;

import com.cts.telemetry.config.OptelConfig;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongUpDownCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import lombok.extern.slf4j.Slf4j;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MemoryMetricsPublisher {

    private static final String METER_NAME = "cts.telemetry";

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cts-telemetry-memory-metrics");
        t.setDaemon(true);
        return t;
    });

    // Instruments
    @SuppressWarnings("unused")
    private ObservableLongUpDownCounter memoryUsedCounter;
    @SuppressWarnings("unused")
    private ObservableLongUpDownCounter memoryCommittedCounter;
    @SuppressWarnings("unused")
    private ObservableLongUpDownCounter memoryLimitCounter;
    @SuppressWarnings("unused")
    private ObservableLongUpDownCounter memoryUsedAfterLastGcCounter;
    @SuppressWarnings("unused")
    private ObservableLongUpDownCounter threadCountCounter;
    @SuppressWarnings("unused")
    private ObservableLongUpDownCounter classCountCounter;
    @SuppressWarnings("unused")
    private DoubleHistogram gcDurationHistogram;

    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final ClassLoadingMXBean classLoadingBean = ManagementFactory.getClassLoadingMXBean();

    public void start(OptelConfig config) {
        if (!config.getMetrics().getMemory().isEnabled()) {
            log.info("Memory metrics disabled.");
            return;
        }

        log.info("Starting MemoryMetricsPublisher with interval {}ms",
                config.getMetrics().getMemory().getPushIntervalMs());

        Meter meter = GlobalOpenTelemetry.getMeter(METER_NAME);
        initializeMetrics(meter);

        scheduler.scheduleAtFixedRate(this::collect, 0,
                config.getMetrics().getMemory().getPushIntervalMs(), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        log.info("Stopping MemoryMetricsPublisher");
        scheduler.shutdown();
    }

    private void initializeMetrics(Meter meter) {

        // ------------------ Memory Metrics ------------------

        memoryUsedCounter = meter.upDownCounterBuilder("jvm.memory.used")
                .setDescription("The amount of used memory")
                .setUnit("By")
                .buildWithCallback(measurement -> {
                    for (MemoryPoolMXBean pool : getMemoryPools()) {
                        MemoryUsage usage = pool.getUsage();
                        if (usage != null) {
                            Attributes attrs = Attributes.builder()
                                    .put("jvm.memory.pool.name", pool.getName())
                                    .put("jvm.memory.type", pool.getType() == MemoryType.HEAP ? "heap" : "non_heap")
                                    .build();
                            measurement.record(usage.getUsed(), attrs);
                        }
                    }
                });

        memoryCommittedCounter = meter.upDownCounterBuilder("jvm.memory.committed")
                .setDescription("The amount of memory that is committed for the Java virtual machine to use")
                .setUnit("By")
                .buildWithCallback(measurement -> {
                    for (MemoryPoolMXBean pool : getMemoryPools()) {
                        MemoryUsage usage = pool.getUsage();
                        if (usage != null) {
                            Attributes attrs = Attributes.builder()
                                    .put("jvm.memory.pool.name", pool.getName())
                                    .put("jvm.memory.type", pool.getType() == MemoryType.HEAP ? "heap" : "non_heap")
                                    .build();
                            measurement.record(usage.getCommitted(), attrs);
                        }
                    }
                });

        memoryLimitCounter = meter.upDownCounterBuilder("jvm.memory.limit")
                .setDescription("The amount of memory that the Java virtual machine will attempt to use")
                .setUnit("By")
                .buildWithCallback(measurement -> {
                    for (MemoryPoolMXBean pool : getMemoryPools()) {
                        MemoryUsage usage = pool.getUsage();
                        if (usage != null && usage.getMax() != -1) {
                            Attributes attrs = Attributes.builder()
                                    .put("jvm.memory.pool.name", pool.getName())
                                    .put("jvm.memory.type", pool.getType() == MemoryType.HEAP ? "heap" : "non_heap")
                                    .build();
                            measurement.record(usage.getMax(), attrs);
                        }
                    }
                });

        memoryUsedAfterLastGcCounter = meter.upDownCounterBuilder("jvm.memory.used_after_last_gc")
                .setDescription("The amount of used memory after the last garbage collection")
                .setUnit("By")
                .buildWithCallback(measurement -> {
                    for (MemoryPoolMXBean pool : getMemoryPools()) {
                        MemoryUsage collectionUsage = pool.getCollectionUsage();
                        if (collectionUsage != null) {
                            Attributes attrs = Attributes.builder()
                                    .put("jvm.memory.pool.name", pool.getName())
                                    .put("jvm.memory.type", pool.getType() == MemoryType.HEAP ? "heap" : "non_heap")
                                    .build();
                            measurement.record(collectionUsage.getUsed(), attrs);
                        }
                    }
                });

        // ------------------ Thread Metrics ------------------

        threadCountCounter = meter.upDownCounterBuilder("jvm.thread.count")
                .setDescription("Number of threads that are currently alive")
                .setUnit("{thread}")
                .buildWithCallback(measurement -> {
                    long[] threadIds = threadBean.getAllThreadIds();
                    ThreadInfo[] threadInfos = threadBean.getThreadInfo(threadIds);
                    Map<Attributes, Long> counts = new HashMap<>();

                    for (ThreadInfo info : threadInfos) {
                        if (info == null)
                            continue;
                        Attributes attrs = Attributes.builder()
                                .put("jvm.thread.state", info.getThreadState().name())
                                .put("jvm.thread.daemon", info.isDaemon())
                                .build();
                        counts.merge(attrs, 1L, Long::sum);
                    }

                    counts.forEach((attrs, count) -> measurement.record(count, attrs));
                });

        // ------------------ Class Metrics ------------------

        classCountCounter = meter.upDownCounterBuilder("jvm.class.count")
                .setDescription("Number of classes that are currently loaded")
                .setUnit("{class}")
                .buildWithCallback(measurement -> {
                    measurement.record(classLoadingBean.getLoadedClassCount(), Attributes.empty());
                });

        // ------------------ GC Metrics ------------------

        gcDurationHistogram = meter.histogramBuilder("jvm.gc.duration")
                .setDescription("The duration of garbage collection")
                .setUnit("s")
                .build();

        registerGcListeners();
    }

    private void registerGcListeners() {
        for (java.lang.management.GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (gcBean instanceof javax.management.NotificationEmitter) {
                javax.management.NotificationEmitter emitter = (javax.management.NotificationEmitter) gcBean;
                emitter.addNotificationListener((notification, handback) -> {
                    if (notification.getType().equals(
                            com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
                        javax.management.openmbean.CompositeData cd = (javax.management.openmbean.CompositeData) notification
                                .getUserData();
                        com.sun.management.GarbageCollectionNotificationInfo info = com.sun.management.GarbageCollectionNotificationInfo
                                .from(cd);

                        long durationMs = info.getGcInfo().getDuration();
                        double durationSec = durationMs / 1000.0;

                        Attributes attrs = Attributes.builder()
                                .put("jvm.gc.name", info.getGcName())
                                .put("jvm.gc.action", info.getGcAction())
                                .build();

                        gcDurationHistogram.record(durationSec, attrs);
                    }
                }, null, null);
            }
        }
    }

    private void collect() {
        // Scheduler triggers this method, but metric collection is handled by OTel SDK
        // callbacks.
        // This method is kept to preserve the existing scheduler logic as requested.
    }

    protected java.util.List<MemoryPoolMXBean> getMemoryPools() {
        return ManagementFactory.getMemoryPoolMXBeans();
    }
}
