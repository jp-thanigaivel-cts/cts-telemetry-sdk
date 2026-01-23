package com.cts.telemetry.adapter.jersey;

import com.cts.telemetry.api.constants.DependencyErrorCodes;
import com.cts.telemetry.api.constants.DependencyErrorMessages;
import com.cts.telemetry.api.exception.OptelDependencyException;
import com.cts.telemetry.config.OptelConfig;
import com.cts.telemetry.metrics.MemoryMetricsPublisher;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OptelJerseyFeature implements Feature {

    @Inject
    private OptelConfig config;

    // Singleton recorder instance for this feature
    private static final MemoryMetricsPublisher recorder = new MemoryMetricsPublisher();

    @Override
    public boolean configure(FeatureContext context) {
        if (config != null) {
            validateDependencies();
            recorder.start(config);
        }
        return true;
    }

    private void validateDependencies() {
        if (config.getTracing() == null || !config.getTracing().isEnabled()) {
            return;
        }

        if (config.getTracing().getInstrument().getDb().isEnabled()) {
            try {
                Class.forName("javax.sql.DataSource");
            } catch (ClassNotFoundException e) {
                throw new OptelDependencyException(
                        DependencyErrorCodes.MISSING_DATASOURCE_DEPENDENCY,
                        DependencyErrorMessages.MISSING_DATASOURCE_DEPENDENCY,
                        e);
            }
        }

        if (config.getTracing().getInstrument().getGrpc().isEnabled()) {
            try {
                Class.forName("io.grpc.ManagedChannel");
            } catch (ClassNotFoundException e) {
                throw new OptelDependencyException(
                        DependencyErrorCodes.MISSING_GRPC_DEPENDENCY,
                        DependencyErrorMessages.MISSING_GRPC_DEPENDENCY,
                        e);
            }
        }
    }
}
