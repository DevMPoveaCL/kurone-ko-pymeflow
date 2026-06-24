package com.kuroneko.pymeflow.application.port.out;

public record ProviderAuth(String providerType, String credentialRef) {
    public ProviderAuth {
        if (providerType == null || providerType.isBlank()) {
            throw new IllegalArgumentException("Provider type is required");
        }
        if (credentialRef == null || credentialRef.isBlank()) {
            throw new IllegalArgumentException("Credential reference is required");
        }
        providerType = providerType.trim();
        credentialRef = credentialRef.trim();
    }
}
