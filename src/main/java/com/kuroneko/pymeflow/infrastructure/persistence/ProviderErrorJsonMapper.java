package com.kuroneko.pymeflow.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuroneko.pymeflow.application.port.out.ProviderError;

import java.util.List;

final class ProviderErrorJsonMapper {
    private final ObjectMapper objectMapper;

    ProviderErrorJsonMapper() {
        this(new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    ProviderErrorJsonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    String serialize(List<ProviderError> errors) {
        try {
            return objectMapper.writeValueAsString(List.copyOf(errors == null ? List.of() : errors).stream()
                    .map(SafeProviderError::from)
                    .toList());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize provider errors", exception);
        }
    }

    List<ProviderError> deserialize(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readerForListOf(SafeProviderError.class)
                    .<List<SafeProviderError>>readValue(json)
                    .stream()
                    .map(SafeProviderError::toProviderError)
                    .flatMap(java.util.Optional::stream)
                    .toList();
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            return List.of();
        }
    }

    private record SafeProviderError(String code, String message, String field, Integer retryAfterSeconds) {
        static SafeProviderError from(ProviderError error) {
            return switch (error) {
                case ProviderError.AuthError auth -> new SafeProviderError("AUTH", auth.safeMessage(), null, null);
                case ProviderError.RateLimitError rateLimit -> new SafeProviderError(
                        "RATE_LIMIT",
                        rateLimit.safeMessage(),
                        null,
                        rateLimit.retryAfterSeconds()
                );
                case ProviderError.UnavailableError unavailable -> new SafeProviderError(
                        "UNAVAILABLE",
                        unavailable.safeMessage(),
                        null,
                        null
                );
                case ProviderError.DataError data -> new SafeProviderError("DATA", data.detail(), data.field(), null);
            };
        }

        java.util.Optional<ProviderError> toProviderError() {
            try {
                return switch (code) {
                    case "AUTH" -> hasText(message)
                            ? java.util.Optional.of(new ProviderError.AuthError(message))
                            : java.util.Optional.empty();
                    case "RATE_LIMIT" -> hasText(message) && retryAfterSeconds != null && retryAfterSeconds >= 0
                            ? java.util.Optional.of(new ProviderError.RateLimitError(retryAfterSeconds, message))
                            : java.util.Optional.empty();
                    case "UNAVAILABLE" -> hasText(message)
                            ? java.util.Optional.of(new ProviderError.UnavailableError(message))
                            : java.util.Optional.empty();
                    case "DATA" -> hasText(message) && hasText(field) && isSafeField(field)
                            ? java.util.Optional.of(new ProviderError.DataError(field, message))
                            : java.util.Optional.empty();
                    default -> java.util.Optional.empty();
                };
            } catch (IllegalArgumentException exception) {
                return java.util.Optional.empty();
            }
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }

        private static boolean isSafeField(String value) {
            var normalized = value.toLowerCase(java.util.Locale.ROOT);
            return !normalized.contains("password")
                    && !normalized.contains("token")
                    && !normalized.contains("secret")
                    && !normalized.contains("credential");
        }
    }
}
