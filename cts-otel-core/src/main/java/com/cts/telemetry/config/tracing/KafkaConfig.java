package com.cts.telemetry.config.tracing;

import lombok.Data;

@Data
public class KafkaConfig {
    private boolean producer = false;
    private boolean consumer = false;
}
