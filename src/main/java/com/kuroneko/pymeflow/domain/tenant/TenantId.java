package com.kuroneko.pymeflow.domain.tenant;

import java.util.UUID;

public record TenantId(UUID value) {
    public TenantId {
        if (value == null) {
            throw new IllegalArgumentException("Tenant id is required");
        }
    }
}
