package com.kuroneko.pymeflow.domain.vertical;

public record ProfileId(String value) {
    public ProfileId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Profile id is required");
        }
        if (!value.matches("[a-z0-9][a-z0-9-]{1,62}")) {
            throw new IllegalArgumentException("Profile id must be kebab-case and between 2 and 63 characters");
        }
    }
}
