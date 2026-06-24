package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.time.LocalDate;
import java.util.Optional;

public record ProviderSyncQuery(
        ProfileId profileId,
        LocalDate dateFrom,
        LocalDate dateTo,
        Optional<String> cursor,
        int pageSize
) {
    public ProviderSyncQuery {
        if (profileId == null) {
            throw new IllegalArgumentException("Profile id is required");
        }
        if (dateFrom == null) {
            throw new IllegalArgumentException("Date from is required");
        }
        if (dateTo == null) {
            throw new IllegalArgumentException("Date to is required");
        }
        if (dateFrom.isAfter(dateTo)) {
            throw new IllegalArgumentException("Date from must be on or before date to");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        cursor = cursor == null ? Optional.empty() : cursor.filter(value -> !value.isBlank());
    }
}
