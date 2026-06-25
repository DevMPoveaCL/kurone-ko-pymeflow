package com.kuroneko.pymeflow.infrastructure.observability;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kuroneko.pymeflow.application.port.out.ProviderSyncObservationPort;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderSyncObservabilityAdapterTest {

    @Test
    void completedObservationRecordsBoundedMetricsAndSafeLifecycleLog() {
        var registry = new SimpleMeterRegistry();
        var adapter = new ProviderSyncObservabilityAdapter(registry);
        var appender = attachLogCapture();

        adapter.observe(new ProviderSyncObservationPort.ProviderSyncObservation(
                ProviderSyncObservationPort.LifecycleEvent.COMPLETED,
                "sync-123",
                "retail-cl",
                "fixture-provider",
                ProviderSyncObservationPort.SyncStatus.COMPLETED,
                2,
                4,
                4,
                false,
                false,
                false,
                ProviderSyncObservationPort.ErrorCode.NONE,
                Optional.empty(),
                Optional.of(Duration.ofMillis(250))
        ));

        assertThat(registry.find("provider.sync.attempts").tags("providerType", "fixture-provider", "status", "COMPLETED", "errorCode", "NONE").counter())
                .extracting(counter -> counter.count())
                .isEqualTo(1.0);
        var timer = registry.find("provider.sync.duration").timer();
        assertThat(timer).isNotNull();
        assertThat(tagKeys(timer)).containsExactlyInAnyOrder("providerType", "status", "errorCode");
        assertThat(logMessages(appender))
                .anySatisfy(message -> assertThat(message)
                        .contains("event=COMPLETED")
                        .contains("syncId=sync-123")
                        .contains("profileId=retail-cl")
                        .contains("providerType=fixture-provider")
                        .doesNotContain("credential")
                        .doesNotContain("cursor")
                        .doesNotContain("payload"));
    }

    @Test
    void failedObservationUsesStableErrorCodeWithoutRawExceptionOrHighCardinalityTags() {
        var registry = new SimpleMeterRegistry();
        var adapter = new ProviderSyncObservabilityAdapter(registry);
        var appender = attachLogCapture();

        adapter.observe(new ProviderSyncObservationPort.ProviderSyncObservation(
                ProviderSyncObservationPort.LifecycleEvent.FAILED,
                "sync-raw-message-999",
                "profile-secret-id",
                "fixture-provider",
                ProviderSyncObservationPort.SyncStatus.FAILED,
                0,
                0,
                0,
                false,
                false,
                false,
                ProviderSyncObservationPort.ErrorCode.RATE_LIMIT,
                Optional.of(120),
                Optional.of(Duration.ofMillis(25))
        ));

        var counter = registry.find("provider.sync.attempts")
                .tags("providerType", "fixture-provider", "status", "FAILED", "errorCode", "RATE_LIMIT")
                .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.getId().getTags()).extracting(tag -> tag.getKey())
                .containsExactlyInAnyOrder("providerType", "status", "errorCode");
        assertThat(counter.getId().getTags()).extracting(tag -> tag.getKey())
                .doesNotContain("syncId", "profileId", "message", "exception", "cursor");
        assertThat(logMessages(appender))
                .anySatisfy(message -> assertThat(message)
                        .contains("event=FAILED")
                        .contains("errorCode=RATE_LIMIT")
                        .doesNotContain("Exception")
                        .doesNotContain("stacktrace")
                        .doesNotContain("raw provider"));
    }

    private static ListAppender<ILoggingEvent> attachLogCapture() {
        var logger = (Logger) LoggerFactory.getLogger(ProviderSyncObservabilityAdapter.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.setLevel(Level.INFO);
        logger.addAppender(appender);
        return appender;
    }

    private static java.util.List<String> tagKeys(Timer timer) {
        return timer.getId().getTags().stream().map(tag -> tag.getKey()).toList();
    }

    private static java.util.List<String> logMessages(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
