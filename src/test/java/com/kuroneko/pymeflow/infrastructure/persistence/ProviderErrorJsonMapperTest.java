package com.kuroneko.pymeflow.infrastructure.persistence;

import com.kuroneko.pymeflow.application.port.out.ProviderError;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderErrorJsonMapperTest {

    private final ProviderErrorJsonMapper mapper = new ProviderErrorJsonMapper();

    @Test
    void roundTripsOnlySafeProviderErrorFields() {
        List<ProviderError> errors = List.of(
                new ProviderError.AuthError("Credential reference is invalid"),
                new ProviderError.RateLimitError(45, "Request limit reached"),
                new ProviderError.UnavailableError("Provider is unavailable"),
                new ProviderError.DataError("amount", "Amount is invalid")
        );

        var json = mapper.serialize(errors);

        assertThat(json)
                .contains("AUTH", "RATE_LIMIT", "UNAVAILABLE", "DATA", "retryAfterSeconds", "field")
                .doesNotContain("password", "token", "stackTrace", "rawPayload");
        assertThat(mapper.deserialize(json)).containsExactlyElementsOf(errors);
    }

    @Test
    void filtersUnknownMalformedAndSecretBearingEntriesDuringDeserialize() {
        var json = """
                [
                  {"code":"RATE_LIMIT","message":"Wait","retryAfterSeconds":30},
                  {"code":"UNKNOWN","message":"raw token abc123"},
                  {"code":"AUTH","message":"   "},
                  {"code":"DATA","message":"Amount failed","field":"password"},
                  {"code":"UNAVAILABLE","message":"Provider is down","stackTrace":"secret-stack"}
                ]
                """;

        assertThat(mapper.deserialize(json)).containsExactly(
                new ProviderError.RateLimitError(30, "Wait"),
                new ProviderError.UnavailableError("Provider is down")
        );
    }

    @Test
    void returnsEmptyListForBlankOrMalformedJson() {
        assertThat(mapper.deserialize("   ")).isEmpty();
        assertThat(mapper.deserialize("{not-json-and-password=secret}")).isEmpty();
    }
}
