package com.cts.telemetry.config;

import com.cts.telemetry.config.tracing.*;
import lombok.Data;

@Data
public class InstrumentConfig {
    private HttpConfig http = new HttpConfig();
    private DbConfig db = new DbConfig();
    private CacheConfig cache = new CacheConfig();
    private InternalConfig internal = new InternalConfig();
    private KafkaConfig kafka = new KafkaConfig();
    private JmsConfig jms = new JmsConfig();
    private GrpcConfig grpc = new GrpcConfig();
}
