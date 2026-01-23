package com.cts.telemetry.config;

import lombok.Data;

@Data
public class TracingConfig {
    private boolean enabled = false;
    private InstrumentConfig instrument = new InstrumentConfig();
}
