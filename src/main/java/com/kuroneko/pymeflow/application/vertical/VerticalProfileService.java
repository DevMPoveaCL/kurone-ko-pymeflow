package com.kuroneko.pymeflow.application.vertical;

import com.kuroneko.pymeflow.application.port.out.ProfileRegistryPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.ProfileRule;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;

import java.util.HashSet;
import java.util.List;

public final class VerticalProfileService {
    private final ProfileRegistryPort profileRegistryPort;

    public VerticalProfileService(ProfileRegistryPort profileRegistryPort) {
        this.profileRegistryPort = profileRegistryPort;
    }

    public VerticalProfile loadProfile(ProfileId profileId) {
        var profile = profileRegistryPort.loadProfile(profileId)
                .orElseThrow(() -> new IllegalArgumentException("Profile not found: " + profileId.value()));
        validateRuleSet(profile);
        return profile;
    }

    public List<ProfileRule> activeRules(ProfileId profileId) {
        return loadProfile(profileId).rules();
    }

    private static void validateRuleSet(VerticalProfile profile) {
        var ruleKeys = new HashSet<String>();
        var actionKeys = new HashSet<String>();

        for (ProfileRule rule : profile.rules()) {
            if (!ruleKeys.add(rule.ruleKey())) {
                throw new IllegalArgumentException("Duplicate rule key: " + rule.ruleKey());
            }
            if (!actionKeys.add(rule.actionKey())) {
                throw new IllegalArgumentException("Duplicate action key: " + rule.actionKey());
            }
        }
    }
}
