package com.cts.telemetry.metrics;

import com.cts.telemetry.api.AppAttributes;
import com.cts.telemetry.api.JvmAttributes;
import org.junit.jupiter.api.Test;

public class MetricsTest {

    @Test
    public void testMetricAttributes() {
        assert JvmAttributes.MEMORY_USED.key().equals("jvm.memory.used");
        assert AppAttributes.SERVICE_NAME.key().equals("service.name");
    }
}
