package com.kuroneko.pymeflow.infrastructure.observability;

import com.kuroneko.pymeflow.application.port.out.ProviderSyncObservationPort;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class ProviderSyncObservabilityAdapter implements ProviderSyncObservationPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderSyncObservabilityAdapter.class);
    private static final String ATTEMPTS_METRIC = "provider.sync.attempts";
    private static final String DURATION_METRIC = "provider.sync.duration";

    private final MeterRegistry meterRegistry;

    public ProviderSyncObservabilityAdapter(MeterRegistry meterRegistry) {
        if (meterRegistry == null) {
            throw new IllegalArgumentException("Meter registry is required");
        }
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void observe(ProviderSyncObservation observation) {
        if (observation == null) {
            return;
        }
        log(observation);
        if (isTerminal(observation)) {
            recordMetrics(observation);
        }
    }

    private void log(ProviderSyncObservation observation) {
        LOGGER.info(
                "providerSync event={} status={} syncId={} profileId={} providerType={} pagesFetched={} entriesFetched={} importedEntries={} hasMorePages={} truncated={} authAborted={} errorCode={} retryAfterSeconds={} durationMillis={}",
                observation.event(),
                observation.status(),
                observation.syncId(),
                observation.profileId(),
                observation.providerType(),
                observation.pagesFetched(),
                observation.entriesFetched(),
                observation.importedEntries(),
                observation.hasMorePages(),
                observation.truncated(),
                observation.authAborted(),
                observation.errorCode(),
                observation.retryAfterSeconds().map(String::valueOf).orElse(""),
                observation.duration().map(duration -> String.valueOf(duration.toMillis())).orElse("")
        );
    }

    private void recordMetrics(ProviderSyncObservation observation) {
        var tags = tagsFor(observation);
        meterRegistry.counter(ATTEMPTS_METRIC, tags).increment();
        observation.duration().ifPresent(duration -> Timer.builder(DURATION_METRIC)
                .tags(tags)
                .register(meterRegistry)
                .record(duration));
    }

    private static boolean isTerminal(ProviderSyncObservation observation) {
        return observation.event() == LifecycleEvent.COMPLETED || observation.event() == LifecycleEvent.FAILED;
    }

    private static List<Tag> tagsFor(ProviderSyncObservation observation) {
        return List.of(
                Tag.of("providerType", observation.providerType()),
                Tag.of("status", observation.status().name()),
                Tag.of("errorCode", observation.errorCode().name())
        );
    }
}
