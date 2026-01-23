package com.cts.telemetry.config.tracing;

import lombok.Data;

@Data
public class DbConfig {
    private boolean enabled = false;
    private boolean captureSql = true;
    private boolean captureParams = false;
}
