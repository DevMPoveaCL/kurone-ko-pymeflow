package com.kuroneko.pymeflow.interfaces.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuroneko.pymeflow.application.cashflow.ProviderSyncStatusUseCase;
import com.kuroneko.pymeflow.application.cashflow.ProviderSyncUseCase;
import com.kuroneko.pymeflow.application.port.out.ProviderAuth;
import com.kuroneko.pymeflow.application.port.out.ProviderError;
import com.kuroneko.pymeflow.application.port.out.SyncSessionPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashflowProviderSyncController.class)
class CashflowProviderSyncControllerTest {
    private static final String ENDPOINT = "/api/cashflow/provider-syncs";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @MockBean
    private ProviderSyncUseCase providerSyncUseCase;

    @MockBean
    private ProviderSyncStatusUseCase providerSyncStatusUseCase;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void postTriggersSyncAndReturnsSafeDurableReportWithoutCredentialEcho() throws Exception {
        when(providerSyncUseCase.sync(any())).thenReturn(new ProviderSyncUseCase.ProviderSyncReport(
                "sync-santander-001",
                2,
                4,
                3,
                false,
                false,
                false,
                List.of(),
                Optional.empty()
        ));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("santander", "fixture-ref-safe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncId").value("sync-santander-001"))
                .andExpect(jsonPath("$.profileId").value("pharmacy-cl"))
                .andExpect(jsonPath("$.providerType").value("santander"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.pagesFetched").value(2))
                .andExpect(jsonPath("$.entriesFetched").value(4))
                .andExpect(jsonPath("$.importedEntries").value(3))
                .andExpect(jsonPath("$.hasMorePages").value(false))
                .andExpect(jsonPath("$.truncated").value(false))
                .andExpect(jsonPath("$.authAborted").value(false))
                .andExpect(jsonPath("$.sessionEntryCount").value(4))
                .andExpect(jsonPath("$.durability").value("DURABLE"))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(content().string(not(containsString("credentialRef"))))
                .andExpect(content().string(not(containsString("fixture-ref-safe"))));

        var command = capturedCommand();
        assertThat(command.profileId()).isEqualTo(new ProfileId("pharmacy-cl"));
        assertThat(command.dateFrom()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(command.dateTo()).isEqualTo(LocalDate.of(2026, 6, 30));
        assertThat(command.auth()).isEqualTo(new ProviderAuth("santander", "fixture-ref-safe"));
    }

    @Test
    void rejectsMissingFieldsWithoutInvokingSync() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'profileId')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'providerType')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'credentialRef')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'dateFrom')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'dateTo')]").exists());

        verify(providerSyncUseCase, never()).sync(any());
    }

    @Test
    void rejectsInvalidDatesWithoutEchoingInput() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("santander", "fixture-ref-safe")
                                .replace("2026-06-01", "01-06-2026")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("dateFrom"))
                .andExpect(content().string(not(containsString("01-06-2026"))));

        verify(providerSyncUseCase, never()).sync(any());
    }

    @Test
    void rejectsDateRangeAndUnsupportedProviderWithoutInvokingSync() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("production-bank", "fixture-ref-safe")
                                .replace("2026-06-01", "2026-07-01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'providerType')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'dateTo')]").exists())
                .andExpect(content().string(not(containsString("production-bank"))));

        verify(providerSyncUseCase, never()).sync(any());
    }

    @Test
    void getReturnsLastDurableStatusSnapshotWithNormalizedProviderErrors() throws Exception {
        var snapshot = new SyncSessionPort.SyncSessionSnapshot(
                "sync-rate-limited-001",
                new ProfileId("pharmacy-cl"),
                "santander",
                SyncSessionPort.SyncStatus.PARTIAL,
                1,
                2,
                2,
                true,
                true,
                false,
                Optional.of("page-2"),
                Optional.of(Instant.parse("2026-06-23T10:15:30Z")),
                5,
                List.of(new ProviderError.RateLimitError(120, "Request limit reached")),
                Optional.of(120),
                SyncSessionPort.Durability.DURABLE
        );
        when(providerSyncStatusUseCase.find("sync-rate-limited-001")).thenReturn(Optional.of(snapshot));

        mockMvc.perform(get(ENDPOINT + "/sync-rate-limited-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.syncId").value("sync-rate-limited-001"))
                .andExpect(jsonPath("$.profileId").value("pharmacy-cl"))
                .andExpect(jsonPath("$.providerType").value("santander"))
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.cursor").value("page-2"))
                .andExpect(jsonPath("$.lastSyncAt").value("2026-06-23T10:15:30Z"))
                .andExpect(jsonPath("$.sessionEntryCount").value(5))
                .andExpect(jsonPath("$.retryAfterSeconds").value(120))
                .andExpect(jsonPath("$.errors[0].code").value("RATE_LIMIT"))
                .andExpect(jsonPath("$.errors[0].message").value("Request limit reached"))
                .andExpect(jsonPath("$.errors[0].retryAfterSeconds").value(120))
                .andExpect(jsonPath("$.durability").value("DURABLE"));
    }

    @Test
    void getUnknownSyncIdReturnsSafeDurableNotFound() throws Exception {
        when(providerSyncStatusUseCase.find("sync-missing-001")).thenReturn(Optional.empty());

        mockMvc.perform(get(ENDPOINT + "/sync-missing-001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SYNC_STATUS_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("No durable status snapshot was found for this syncId."))
                .andExpect(jsonPath("$.durability").value("DURABLE"));
    }

    @Test
    void getPersistedStatusReturnsOnlySafeProviderErrorFields() throws Exception {
        var snapshot = new SyncSessionPort.SyncSessionSnapshot(
                "sync-safe-errors-001",
                new ProfileId("pharmacy-cl"),
                "santander",
                SyncSessionPort.SyncStatus.FAILED,
                0,
                0,
                0,
                false,
                false,
                true,
                Optional.empty(),
                Optional.of(Instant.parse("2026-06-23T10:15:30Z")),
                0,
                List.of(
                        new ProviderError.AuthError("Credential rejected"),
                        new ProviderError.DataError("amount", "Statement amount is invalid")
                ),
                Optional.empty(),
                SyncSessionPort.Durability.DURABLE
        );
        when(providerSyncStatusUseCase.find("sync-safe-errors-001")).thenReturn(Optional.of(snapshot));

        mockMvc.perform(get(ENDPOINT + "/sync-safe-errors-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.durability").value("DURABLE"))
                .andExpect(jsonPath("$.errors[0].code").value("AUTH_ERROR"))
                .andExpect(jsonPath("$.errors[0].message").value("Credential rejected"))
                .andExpect(jsonPath("$.errors[1].code").value("DATA_ERROR"))
                .andExpect(jsonPath("$.errors[1].field").value("amount"))
                .andExpect(jsonPath("$.errors[1].message").value("Statement amount is invalid"))
                .andExpect(content().string(not(containsString("credentialRef"))))
                .andExpect(content().string(not(containsString("fixture-ref"))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("token"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andExpect(content().string(not(containsString("stackTrace"))))
                .andExpect(content().string(not(containsString("rawPayload"))));
    }

    @Test
    void mapsAllProviderErrorTypesToSafeResponseCodes() throws Exception {
        when(providerSyncUseCase.sync(any())).thenReturn(new ProviderSyncUseCase.ProviderSyncReport(
                "sync-errors-001",
                0,
                0,
                0,
                false,
                false,
                true,
                List.of(
                        new ProviderError.AuthError("Credential rejected"),
                        new ProviderError.UnavailableError("Provider unavailable"),
                        new ProviderError.DataError("currency", "Only CLP provider statement rows are supported")
                ),
                Optional.empty()
        ));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload("santander", "safe-ref")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errors[0].code").value("AUTH_ERROR"))
                .andExpect(jsonPath("$.errors[1].code").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.errors[2].code").value("DATA_ERROR"))
                .andExpect(jsonPath("$.errors[2].field").value("currency"))
                .andExpect(content().string(not(containsString("safe-ref"))));
    }

    @Test
    void documentsOpenApiResponsesAndSafeExamples() throws Exception {
        Method postEndpoint = CashflowProviderSyncController.class.getMethod(
                "trigger",
                CashflowProviderSyncController.ProviderSyncRequest.class
        );
        Method getEndpoint = CashflowProviderSyncController.class.getMethod("status", String.class);

        var postOperation = postEndpoint.getAnnotation(Operation.class);
        var postResponses = postEndpoint.getAnnotation(ApiResponses.class).value();
        var getResponses = getEndpoint.getAnnotation(ApiResponses.class).value();
        var example = requestExample(postEndpoint.getParameters()[0].getAnnotation(RequestBody.class));
        var payload = OBJECT_MAPPER.readTree(example.value());

        assertThat(postOperation.description())
                .contains("fixture-backed")
                .contains("does not accept raw credentials")
                .contains("durable");
        assertThat(getEndpoint.getAnnotation(Operation.class).description())
                .contains("last safe durable status snapshot")
                .doesNotContain("in-memory", "process restarted", "non-durable");
        assertThat(postResponses).extracting(response -> response.responseCode()).contains("200", "400");
        assertThat(getResponses).extracting(response -> response.responseCode()).contains("200", "404");
        assertThat(payload.path("credentialRef").asText()).isEqualTo("fixture-ref-santander");
        assertThat(example.value()).doesNotContain("secret", "token", "password");
    }

    @Test
    void exposesOnlyTriggerAndStatusRoutesWithoutOperatorActionSurface() {
        var providerSyncRoutes = handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> isProviderSyncController(entry.getValue()))
                .flatMap(entry -> entry.getKey().getPatternValues().stream()
                        .flatMap(pattern -> entry.getKey().getMethodsCondition().getMethods().stream()
                                .map(method -> route(method, pattern))))
                .collect(java.util.stream.Collectors.toSet());

        assertThat(providerSyncRoutes)
                .containsExactlyInAnyOrder(
                        "POST /api/cashflow/provider-syncs",
                        "GET /api/cashflow/provider-syncs/{syncId}"
                );
        assertThat(providerSyncRoutes)
                .allSatisfy(route -> assertThat(route.toLowerCase(Locale.ROOT))
                        .doesNotContain("list", "audit", "retry", "manual", "operator", "ui"));
    }

    private ProviderSyncUseCase.ProviderSyncCommand capturedCommand() {
        var captor = ArgumentCaptor.forClass(ProviderSyncUseCase.ProviderSyncCommand.class);
        verify(providerSyncUseCase).sync(captor.capture());
        return captor.getValue();
    }

    private static ExampleObject requestExample(RequestBody requestBody) {
        assertThat(requestBody).isNotNull();
        return requestBody.content()[0].examples()[0];
    }

    private static boolean isProviderSyncController(HandlerMethod handlerMethod) {
        return handlerMethod.getBeanType().equals(CashflowProviderSyncController.class);
    }

    private static String route(RequestMethod method, String pattern) {
        return method.name() + " " + pattern;
    }

    private static String validPayload(String providerType, String credentialRef) {
        return """
                {
                  "profileId": "pharmacy-cl",
                  "providerType": "%s",
                  "credentialRef": "%s",
                  "dateFrom": "2026-06-01",
                  "dateTo": "2026-06-30"
                }
                """.formatted(providerType, credentialRef);
    }
}
