package com.kuroneko.pymeflow.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record ProviderSyncPage(
        List<ExternalStatementEntry> entries,
        Optional<String> nextCursor,
        Optional<Integer> totalPagesEstimate,
        Optional<Instant> rateLimitResetsAt
) {
    public ProviderSyncPage {
        if (entries == null) {
            throw new IllegalArgumentException("Entries are required");
        }
        entries = List.copyOf(entries);
        nextCursor = nextCursor == null ? Optional.empty() : nextCursor.filter(value -> !value.isBlank());
        totalPagesEstimate = totalPagesEstimate == null ? Optional.empty() : totalPagesEstimate;
        rateLimitResetsAt = rateLimitResetsAt == null ? Optional.empty() : rateLimitResetsAt;
    }
}
