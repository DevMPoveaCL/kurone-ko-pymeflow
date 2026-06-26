package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.cockpit.CockpitPreferences;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.util.Optional;

public interface CockpitPreferencesPort {
    Optional<CockpitPreferences> findByProfile(ProfileId profileId);

    void save(ProfileId profileId, CockpitPreferences preferences);
}
