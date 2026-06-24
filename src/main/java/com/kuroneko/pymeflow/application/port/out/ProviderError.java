package com.kuroneko.pymeflow.application.port.out;

public sealed interface ProviderError permits
        ProviderError.AuthError,
        ProviderError.RateLimitError,
        ProviderError.UnavailableError,
        ProviderError.DataError {

    record AuthError(String safeMessage) implements ProviderError {
        public AuthError {
            safeMessage = requireText(safeMessage, "Safe message");
        }
    }

    record RateLimitError(int retryAfterSeconds, String safeMessage) implements ProviderError {
        public RateLimitError {
            if (retryAfterSeconds < 0) {
                throw new IllegalArgumentException("Retry after seconds must not be negative");
            }
            safeMessage = requireText(safeMessage, "Safe message");
        }
    }

    record UnavailableError(String safeMessage) implements ProviderError {
        public UnavailableError {
            safeMessage = requireText(safeMessage, "Safe message");
        }
    }

    record DataError(String field, String detail) implements ProviderError {
        public DataError {
            field = requireText(field, "Field");
            detail = requireText(detail, "Detail");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
