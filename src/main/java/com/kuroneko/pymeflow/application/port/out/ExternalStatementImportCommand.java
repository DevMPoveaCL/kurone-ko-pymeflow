package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.util.List;

public record ExternalStatementImportCommand(
        ProfileId profileId,
        String importLabel,
        List<ExternalStatementEntry> entries
) {
    public ExternalStatementImportCommand {
        if (profileId == null) {
            throw new IllegalArgumentException("Profile id is required");
        }
        entries = List.copyOf(entries == null ? List.of() : entries);
    }
}
