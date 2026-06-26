package com.kuroneko.pymeflow.application.cockpit;

import com.kuroneko.pymeflow.application.port.out.CockpitPreferencesPort;
import com.kuroneko.pymeflow.domain.cockpit.CockpitPreferences;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

public class CockpitPreferencesService {
    private final CockpitPreferencesPort port;

    public CockpitPreferencesService(CockpitPreferencesPort port) {
        this.port = port;
    }

    @Transactional(readOnly = true)
    public Optional<CockpitPreferences> load(ProfileId profileId) {
        return port.findByProfile(profileId);
    }

    @Transactional
    public CockpitPreferences save(ProfileId profileId, BigDecimal openingBalance, int preferredHorizonDays) {
        var preferences = new CockpitPreferences(openingBalance, preferredHorizonDays);
        port.save(profileId, preferences);
        return preferences;
    }
}
