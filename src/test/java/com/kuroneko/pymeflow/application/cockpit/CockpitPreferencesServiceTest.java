package com.kuroneko.pymeflow.application.cockpit;

import com.kuroneko.pymeflow.application.port.out.CockpitPreferencesPort;
import com.kuroneko.pymeflow.domain.cockpit.CockpitPreferences;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CockpitPreferencesServiceTest {
    private static final ProfileId PROFILE_ID = new ProfileId("retail-cl");

    @Test
    void rejectsUnsupportedHorizonBeforeSaving() {
        var port = mock(CockpitPreferencesPort.class);
        var service = new CockpitPreferencesService(port);

        assertThatThrownBy(() -> service.save(PROFILE_ID, BigDecimal.valueOf(150_000), 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El horizonte debe ser 7 o 30 días.");

        verify(port, never()).save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsNegativeOpeningBalanceBeforeSaving() {
        var port = mock(CockpitPreferencesPort.class);
        var service = new CockpitPreferencesService(port);

        assertThatThrownBy(() -> service.save(PROFILE_ID, BigDecimal.valueOf(-1), 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El saldo inicial no puede ser negativo.");

        verify(port, never()).save(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void savesValidPreferencesThroughPort() {
        var port = mock(CockpitPreferencesPort.class);
        var service = new CockpitPreferencesService(port);

        var saved = service.save(PROFILE_ID, BigDecimal.valueOf(250_000), 30);

        assertThat(saved.openingBalance()).isEqualByComparingTo("250000");
        assertThat(saved.preferredHorizonDays()).isEqualTo(30);
        verify(port).save(PROFILE_ID, saved);
    }

    @Test
    void loadsExistingPreferencesByProfile() {
        var port = mock(CockpitPreferencesPort.class);
        var service = new CockpitPreferencesService(port);
        var preferences = new CockpitPreferences(BigDecimal.valueOf(150_000), 7);
        when(port.findByProfile(PROFILE_ID)).thenReturn(Optional.of(preferences));

        var loaded = service.load(PROFILE_ID);

        assertThat(loaded).contains(preferences);
    }
}
