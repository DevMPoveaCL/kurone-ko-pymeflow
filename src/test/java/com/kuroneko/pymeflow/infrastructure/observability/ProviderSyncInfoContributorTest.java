package com.kuroneko.pymeflow.infrastructure.observability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderSyncInfoContributorTest {

    @Test
    void contributesStorageOnlyProviderSyncCapabilityMetadata() {
        var builder = new Info.Builder();

        new ProviderSyncInfoContributor().contribute(builder);

        assertThat(builder.build().getDetails())
                .containsKey("providerSync")
                .extractingByKey("providerSync")
                .isEqualTo(Map.of(
                        "scope", "storage-only",
                        "storage", "provider_sync_sessions",
                        "capability", "durable-provider-sync-sessions",
                        "healthIndicator", "providerSyncStorage"
                ));
    }

    @Test
    void doesNotPublishExternalProviderCredentialNetworkOrPayloadClaims() {
        var builder = new Info.Builder();

        new ProviderSyncInfoContributor().contribute(builder);

        var details = builder.build().getDetails().toString();

        assertThat(details)
                .doesNotContain(
                        "bankConnectivity",
                        "providerConnectivity",
                        "networkConnectivity",
                        "sandbox",
                        "production",
                        "credentialRef",
                        "credentialStatus",
                        "rawPayload",
                        "cursor"
                );
    }
}
