package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

public interface DemoDataPort {
    void reset(ProfileId profileId);
}
