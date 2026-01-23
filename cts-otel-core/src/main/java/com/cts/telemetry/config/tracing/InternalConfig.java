package com.cts.telemetry.config.tracing;

import lombok.Data;

@Data
public class InternalConfig {
    private boolean enabled = false;
    private String[] packages = new String[0];
}
