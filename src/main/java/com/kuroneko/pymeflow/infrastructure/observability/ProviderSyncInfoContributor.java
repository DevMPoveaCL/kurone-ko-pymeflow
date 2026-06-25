package com.kuroneko.pymeflow.infrastructure.observability;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public final class ProviderSyncInfoContributor implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("providerSync", Map.of(
                "scope", "storage-only",
                "storage", "provider_sync_sessions",
                "capability", "durable-provider-sync-sessions",
                "healthIndicator", "providerSyncStorage"
        ));
    }
}
