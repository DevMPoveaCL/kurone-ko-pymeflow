package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;

import java.util.Optional;

public interface ProfileRegistryPort {
    Optional<VerticalProfile> loadProfile(ProfileId id);
}
