package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.cashflow.SensitiveDataPolicy;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportCommand;
import com.kuroneko.pymeflow.application.port.out.ExternalStatementImportPort;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashflowBankStatementSimulatedController.class)
class CashflowBankStatementSimulatedControllerTest {
    private static final String ENDPOINT = "/api/cashflow/imports/bank-statement/simulated";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @MockBean
    private ExternalStatementImportPort externalStatementImportPort;

    @MockBean
    private SensitiveDataPolicy sensitiveDataPolicy;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsValidStatement() throws Exception {
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        when(externalStatementImportPort.importStatement(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        transaction("Venta POS", 125000),
                        new CategoryAssignment(Optional.of(category), false)
                )),
                List.of(),
                List.of()
        ));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("BT-100", "2026-06-15", "Venta POS", "125000", "CLP", "Cuenta corriente", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importId").isNotEmpty())
                .andExpect(jsonPath("$.profileId").value("pharmacy-cl"))
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.categorizedCount").value(1))
                .andExpect(jsonPath("$.manualReviewCount").value(0))
                .andExpect(jsonPath("$.rejectedCount").value(0))
                .andExpect(jsonPath("$.invalid").value(0))
                .andExpect(jsonPath("$.categorized[0].movementId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.categorized[0].row").value(1))
                .andExpect(jsonPath("$.categorized[0].transaction.movementDirection").value("CREDIT"));
    }

    @Test
    void rejectsEmptyRows() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accepted").value(0))
                .andExpect(jsonPath("$.invalid").value(1))
                .andExpect(jsonPath("$.errors[0].row").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("rows"));

        verify(externalStatementImportPort, never()).importStatement(any());
    }

    @Test
    void rejectsBlankBankTransactionId() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("   ", "2026-06-15", "Venta POS", "125000", "CLP", "Cuenta corriente", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].row").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("bankTransactionId"))
                .andExpect(jsonPath("$.errors[0].message").value("El identificador bancario es obligatorio."))
                .andExpect(content().string(not(containsString("   "))));
    }

    @Test
    void rejectsZeroAmount() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("BT-101", "2026-06-15", "Venta POS", "0", "CLP", "Cuenta corriente", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[0].message").value("El monto debe ser distinto de cero."));
    }

    @Test
    void rejectsNonClpNoEcho() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("BT-102", "2026-06-15", "Venta POS", "125000", "USD", "Cuenta corriente", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("currency"))
                .andExpect(jsonPath("$.errors[0].message").value("La única moneda soportada es CLP."))
                .andExpect(content().string(not(containsString("USD"))));
    }

    @Test
    void rejectsInvalidDateNoEcho() throws Exception {
        var invalidDate = "15-06-2026";

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("BT-103", invalidDate, "Venta POS", "125000", "CLP", "Cuenta corriente", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("bookingDate"))
                .andExpect(jsonPath("$.errors[0].message").value("La fecha debe tener formato ISO yyyy-MM-dd."))
                .andExpect(content().string(not(containsString(invalidDate))));
    }

    @Test
    void rejectsSensitiveIdNoEcho() throws Exception {
        when(sensitiveDataPolicy.rejectsText("paciente-12345")).thenReturn(true);

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("paciente-12345", "2026-06-15", "Venta POS", "125000", "CLP", "Cuenta corriente", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("bankTransactionId"))
                .andExpect(jsonPath("$.errors[0].message").value("El identificador bancario contiene datos sensibles."))
                .andExpect(content().string(not(containsString("paciente-12345"))));
    }

    @Test
    void mixedBatchPartialSuccess() throws Exception {
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        when(externalStatementImportPort.importStatement(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        transaction("Venta POS", 125000),
                        new CategoryAssignment(Optional.of(category), false)
                )),
                List.of(),
                List.of()
        ));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"bankTransactionId": "BT-104", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 125000, "currency": "CLP", "accountAlias": "Cuenta corriente"},
                                    {"bankTransactionId": "", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 9000, "currency": "CLP", "accountAlias": "Cuenta corriente"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.invalid").value(1))
                .andExpect(jsonPath("$.categorized[0].row").value(1))
                .andExpect(jsonPath("$.errors[0].row").value(2));
    }

    @Test
    void duplicateBankTransactionIdsAreRejectedWhileUniqueRowsContinue() throws Exception {
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        when(externalStatementImportPort.importStatement(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        transaction("Venta POS", 22000),
                        new CategoryAssignment(Optional.of(category), false),
                        "BT-UNIQUE"
                )),
                List.of(),
                List.of()
        ));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"bankTransactionId": "BT-DUPLICATED-SENSITIVE-RISK", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 11000, "currency": "CLP", "accountAlias": "Cuenta corriente"},
                                    {"bankTransactionId": "BT-UNIQUE", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 22000, "currency": "CLP", "accountAlias": "Cuenta corriente"},
                                    {"bankTransactionId": " BT-DUPLICATED-SENSITIVE-RISK ", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 33000, "currency": "CLP", "accountAlias": "Cuenta corriente"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.invalid").value(2))
                .andExpect(jsonPath("$.categorized[0].row").value(2))
                .andExpect(jsonPath("$.errors[0].row").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("bankTransactionId"))
                .andExpect(jsonPath("$.errors[0].message").value("El identificador bancario está duplicado en la solicitud."))
                .andExpect(jsonPath("$.errors[1].row").value(3))
                .andExpect(jsonPath("$.errors[1].field").value("bankTransactionId"))
                .andExpect(jsonPath("$.errors[1].message").value("El identificador bancario está duplicado en la solicitud."))
                .andExpect(content().string(not(containsString("BT-DUPLICATED-SENSITIVE-RISK"))));

        var captor = forClass(ExternalStatementImportCommand.class);
        verify(externalStatementImportPort).importStatement(captor.capture());
        assertThat(captor.getValue().entries())
                .singleElement()
                .satisfies(entry -> assertThat(entry.externalReference()).isEqualTo("BT-UNIQUE"));
    }

    @Test
    void allDuplicateBankTransactionIdsReturnBadRequestWithoutDelegating() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"bankTransactionId": "BT-DUPLICATED-SENSITIVE-RISK", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 11000, "currency": "CLP", "accountAlias": "Cuenta corriente"},
                                    {"bankTransactionId": "BT-DUPLICATED-SENSITIVE-RISK", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 22000, "currency": "CLP", "accountAlias": "Cuenta corriente"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accepted").value(0))
                .andExpect(jsonPath("$.invalid").value(2))
                .andExpect(jsonPath("$.errors[0].row").value(1))
                .andExpect(jsonPath("$.errors[1].row").value(2))
                .andExpect(content().string(not(containsString("BT-DUPLICATED-SENSITIVE-RISK"))));

        verify(externalStatementImportPort, never()).importStatement(any());
    }

    @Test
    void allInvalidReturns400() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"bankTransactionId": "", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 125000, "currency": "CLP", "accountAlias": "Cuenta corriente"},
                                    {"bankTransactionId": "BT-105", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 9000, "currency": "USD", "accountAlias": "Cuenta corriente"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accepted").value(0))
                .andExpect(jsonPath("$.invalid").value(2))
                .andExpect(jsonPath("$.errors[0].row").value(1))
                .andExpect(jsonPath("$.errors[1].row").value(2));
    }

    @Test
    void reimportReturnsExistingId() throws Exception {
        var existingMovementId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        when(externalStatementImportPort.importStatement(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        existingMovementId,
                        transaction("Venta POS", 125000),
                        new CategoryAssignment(Optional.of(category), false)
                )),
                List.of(),
                List.of()
        ));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("BT-106", "2026-06-15", "Venta POS", "125000", "CLP", "Cuenta corriente", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorized[0].movementId").value(existingMovementId.toString()));
    }

    @Test
    void oneBasedRowPositions() throws Exception {
        when(externalStatementImportPort.importStatement(any())).thenReturn(emptyIngestionResult());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"bankTransactionId": "BT-107", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 125000, "currency": "CLP", "accountAlias": "Cuenta corriente"},
                                    {"bankTransactionId": "", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 9000, "currency": "CLP", "accountAlias": "Cuenta corriente"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].row").value(2));

        var captor = forClass(ExternalStatementImportCommand.class);
        verify(externalStatementImportPort).importStatement(captor.capture());
        assertThat(captor.getValue().entries()).singleElement().satisfies(entry -> {
            assertThat(entry.externalReference()).isEqualTo("BT-107");
            assertThat(entry.date()).isEqualTo(LocalDate.of(2026, 6, 15));
            assertThat(entry.amount()).isEqualByComparingTo("125000");
            assertThat(entry.currency()).isEqualTo(Currency.getInstance("CLP"));
            assertThat(entry.accountAlias()).isEqualTo("Cuenta corriente");
        });
    }

    @Test
    void preservesOriginalRowsAcrossMixedResultPartitions() throws Exception {
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        when(externalStatementImportPort.importStatement(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        transaction("Venta POS", 22000),
                        new CategoryAssignment(Optional.of(category), false),
                        "BT-202"
                )),
                List.of(new CashflowIngestionService.ManualReviewTransaction(
                        UUID.fromString("44444444-4444-4444-4444-444444444444"),
                        transaction("Compra insumo", 44000),
                        new CategoryAssignment(Optional.empty(), true),
                        "BT-204"
                )),
                List.of(new CashflowIngestionService.RejectedTransaction(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        transaction("Dato sensible", 11000),
                        "SENSITIVE_IDENTIFIER_REJECTED",
                        "BT-201"
                ))
        ));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"bankTransactionId": "BT-201", "bookingDate": "2026-06-15", "description": "Dato sensible", "amount": 11000, "currency": "CLP", "accountAlias": "Cuenta corriente"},
                                    {"bankTransactionId": "BT-202", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 22000, "currency": "CLP", "accountAlias": "Cuenta corriente"},
                                    {"bankTransactionId": "", "bookingDate": "2026-06-15", "description": "Fila inválida", "amount": 33000, "currency": "CLP", "accountAlias": "Cuenta corriente"},
                                    {"bankTransactionId": "BT-204", "bookingDate": "2026-06-15", "description": "Compra insumo", "amount": 44000, "currency": "CLP", "accountAlias": "Cuenta corriente"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(3))
                .andExpect(jsonPath("$.categorized[0].row").value(2))
                .andExpect(jsonPath("$.manualReview[0].row").value(4))
                .andExpect(jsonPath("$.rejected[0].row").value(1))
                .andExpect(jsonPath("$.errors[0].row").value(3));
    }

    @Test
    void responseEntriesExposeMovementDirectionForAllResultPartitions() throws Exception {
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        when(externalStatementImportPort.importStatement(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        transaction("Venta POS", 22000, TransactionDirection.CREDIT),
                        new CategoryAssignment(Optional.of(category), false),
                        "BT-202"
                )),
                List.of(new CashflowIngestionService.ManualReviewTransaction(
                        UUID.fromString("44444444-4444-4444-4444-444444444444"),
                        transaction("Compra insumo", 44000, TransactionDirection.DEBIT),
                        new CategoryAssignment(Optional.empty(), true),
                        "BT-204"
                )),
                List.of(new CashflowIngestionService.RejectedTransaction(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        transaction("Dato sensible", 11000, TransactionDirection.DEBIT),
                        "SENSITIVE_IDENTIFIER_REJECTED",
                        "BT-201"
                ))
        ));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"bankTransactionId": "BT-201", "bookingDate": "2026-06-15", "description": "Dato sensible", "amount": -11000, "currency": "CLP", "accountAlias": "Cuenta corriente"},
                                    {"bankTransactionId": "BT-202", "bookingDate": "2026-06-15", "description": "Venta POS", "amount": 22000, "currency": "CLP", "accountAlias": "Cuenta corriente"},
                                    {"bankTransactionId": "BT-204", "bookingDate": "2026-06-15", "description": "Compra insumo", "amount": -44000, "currency": "CLP", "accountAlias": "Cuenta corriente"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorized[0].transaction.movementDirection").value("CREDIT"))
                .andExpect(jsonPath("$.manualReview[0].transaction.movementDirection").value("DEBIT"))
                .andExpect(jsonPath("$.rejected[0].movementDirection").value("DEBIT"));
    }

    @Test
    void documentsOpenApiResponsesAndDirectionPreservation() throws Exception {
        Method endpoint = CashflowBankStatementSimulatedController.class.getMethod(
                "importSimulated",
                CashflowBankStatementSimulatedController.SimulatedBankStatementRequest.class
        );

        var responses = endpoint.getAnnotation(ApiResponses.class).value();
        var operation = endpoint.getAnnotation(Operation.class);

        assertThat(responses)
                .extracting(response -> response.responseCode())
                .contains("200", "400");
        assertThat(operation.description())
                .contains("Los montos con signo se normalizan a valores positivos")
                .contains("la dirección del movimiento se conserva como DEBIT o CREDIT");
    }

    @Test
    void documentsOpenApiRequestExampleWithRowLevelBookingDateAndAccountAlias() throws Exception {
        var openApiRequestBody = requestBodyDocumentation();
        var example = requestExample(openApiRequestBody);
        var payload = OBJECT_MAPPER.readTree(example.value());

        assertThat(openApiRequestBody.description()).contains("simulated bank statement");
        assertThat(example.name()).isEqualTo("simulatedBankStatementImport");
        assertThat(payload.path("rows").isArray()).isTrue();
        assertThat(payload.path("rows")).hasSize(1);
        assertThat(payload.path("rows").get(0).path("bookingDate").asText()).isEqualTo("2026-06-15");
        assertThat(payload.path("rows").get(0).path("accountAlias").asText()).isEqualTo("Cuenta corriente");
    }

    @Test
    void documentsOpenApiRequestExampleWithoutBookedAtOrRootAccountAlias() throws Exception {
        var example = requestExample(requestBodyDocumentation());
        var payload = OBJECT_MAPPER.readTree(example.value());

        assertThat(payload.has("accountAlias")).isFalse();
        assertThat(example.value()).doesNotContain("bookedAt");
    }

    private static RequestBody requestBodyDocumentation() throws NoSuchMethodException {
        Method endpoint = CashflowBankStatementSimulatedController.class.getMethod(
                "importSimulated",
                CashflowBankStatementSimulatedController.SimulatedBankStatementRequest.class
        );

        var openApiRequestBody = endpoint.getParameters()[0]
                .getAnnotation(RequestBody.class);
        assertThat(openApiRequestBody).isNotNull();
        return openApiRequestBody;
    }

    private static ExampleObject requestExample(RequestBody openApiRequestBody) {
        return openApiRequestBody.content()[0].examples()[0];
    }

    private static CashflowIngestionService.CashflowIngestionResult emptyIngestionResult() {
        return new CashflowIngestionService.CashflowIngestionResult(List.of(), List.of(), List.of());
    }

    private static String singleRowPayload(
            String bankTransactionId,
            String bookingDate,
            String description,
            String amount,
            String currency,
            String accountAlias,
            String counterpartyName
    ) {
        return """
                {
                  "profileId": "pharmacy-cl",
                  "importLabel": "Cartola junio 2026",
                  "rows": [
                    {
                      "bankTransactionId": %s,
                      "bookingDate": %s,
                      "description": %s,
                      "amount": %s,
                      "currency": %s,
                      "accountAlias": %s,
                      "counterpartyName": %s
                    }
                  ]
                }
                """.formatted(
                jsonString(bankTransactionId),
                jsonString(bookingDate),
                jsonString(description),
                amount,
                jsonString(currency),
                jsonString(accountAlias),
                jsonString(counterpartyName)
        );
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static Transaction transaction(String description, int amount) {
        return transaction(description, amount, TransactionDirection.CREDIT);
    }

    private static Transaction transaction(String description, int amount, TransactionDirection direction) {
        return new Transaction(
                description,
                BigDecimal.valueOf(amount),
                Currency.getInstance("CLP"),
                LocalDate.of(2026, 6, 15),
                direction
        );
    }
}
