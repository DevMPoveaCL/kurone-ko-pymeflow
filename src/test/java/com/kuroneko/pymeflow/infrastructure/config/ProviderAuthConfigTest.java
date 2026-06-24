package com.kuroneko.pymeflow.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderAuthConfigTest {

    @Test
    void appliesSafeDefaultsWhenPagingSettingsAreMissing() {
        var config = new ProviderAuthConfig(null, null);

        assertThat(config.maxPages()).isEqualTo(50);
        assertThat(config.pageSize()).isEqualTo(100);
    }

    @Test
    void keepsExplicitPositivePagingSettings() {
        var config = new ProviderAuthConfig(10, 25);

        assertThat(config.maxPages()).isEqualTo(10);
        assertThat(config.pageSize()).isEqualTo(25);
    }

    @Test
    void rejectsNonPositivePagingSettings() {
        assertThatThrownBy(() -> new ProviderAuthConfig(0, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Max pages must be greater than zero");
        assertThatThrownBy(() -> new ProviderAuthConfig(50, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page size must be greater than zero");
    }
}
