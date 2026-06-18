package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
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
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CashflowManualImportController.class)
class CashflowManualImportControllerTest {

    @MockBean
    private CashflowIngestionService cashflowIngestionService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void acceptsMixedBatchAndReportsRowLevelErrorsWithoutBlockingValidRows() throws Exception {
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        when(cashflowIngestionService.ingest(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        transaction("Venta Caja 1", 125000),
                        new CategoryAssignment(Optional.of(category), false)
                )),
                List.of(),
                List.of()
        ));

        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "importLabel": "Ventas junio 2026",
                                  "rows": [
                                    {"rowNumber": 10, "description": "Venta Caja 1", "amount": 125000, "date": "2026-06-15"},
                                    {"rowNumber": 11, "description": "Devolución", "amount": -5000, "date": "2026-06-15"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileId").value("pharmacy-cl"))
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.categorizedCount").value(1))
                .andExpect(jsonPath("$.invalid").value(1))
                .andExpect(jsonPath("$.categorized[0].movementId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.categorized[0].row").value(10))
                .andExpect(jsonPath("$.errors[0].row").value(11))
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[0].message").value("El monto debe ser mayor que cero."));
    }

    @Test
    void returnsBadRequestWhenAllRowsAreInvalidWithZeroAcceptedRows() throws Exception {
        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"description": "", "amount": -1, "currency": "CLP", "date": "2026-06-15"},
                                    {"description": "Venta Caja 2", "amount": 0, "currency": "USD", "date": "2026-06-15"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.accepted").value(0))
                .andExpect(jsonPath("$.categorizedCount").value(0))
                .andExpect(jsonPath("$.manualReviewCount").value(0))
                .andExpect(jsonPath("$.rejectedCount").value(0))
                .andExpect(jsonPath("$.invalid").value(2))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void validatesBlankDescriptionWithoutEchoingSubmittedValue() throws Exception {
        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("   ", "125000", "CLP", "2026-06-15", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].row").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("description"))
                .andExpect(jsonPath("$.errors[0].message").value("La descripción es obligatoria."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("   "))));
    }

    @Test
    void validatesNonPositiveAmountWithoutEchoingSubmittedValue() throws Exception {
        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("Venta Caja 1", "0", "CLP", "2026-06-15", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].row").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("amount"))
                .andExpect(jsonPath("$.errors[0].message").value("El monto debe ser mayor que cero."));
    }

    @Test
    void validatesInvalidIsoDateWithoutEchoingSubmittedValue() throws Exception {
        var invalidDate = "15-06-2026";

        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("Venta Caja 1", "125000", "CLP", invalidDate, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].row").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("date"))
                .andExpect(jsonPath("$.errors[0].message").value("La fecha debe tener formato ISO yyyy-MM-dd."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(invalidDate))));
    }

    @Test
    void validatesNonClpCurrencyWithoutEchoingSubmittedValue() throws Exception {
        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("Venta Caja 1", "125000", "USD", "2026-06-15", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].row").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("currency"))
                .andExpect(jsonPath("$.errors[0].message").value("La única moneda soportada es CLP."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("USD"))));
    }

    @Test
    void delegatesValidRowsWithProfileIdClpDefaultAndExternalReferencePassthrough() throws Exception {
        when(cashflowIngestionService.ingest(any())).thenReturn(emptyIngestionResult());

        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {
                                      "description": "Venta Caja 1",
                                      "amount": 125000,
                                      "date": "2026-06-15",
                                      "externalReference": " batch-001 "
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        var captor = forClass(CashflowIngestionService.CashflowIngestionCommand.class);
        verify(cashflowIngestionService).ingest(captor.capture());
        assertThat(captor.getValue().profileId().value()).isEqualTo("pharmacy-cl");
        assertThat(captor.getValue().items()).singleElement().satisfies(item -> {
            assertThat(item.transaction().description()).isEqualTo("Venta Caja 1");
            assertThat(item.transaction().amount()).isEqualByComparingTo("125000");
            assertThat(item.transaction().currency()).isEqualTo(Currency.getInstance("CLP"));
            assertThat(item.transaction().bookedAt()).isEqualTo(LocalDate.of(2026, 6, 15));
            assertThat(item.externalReference()).isEqualTo("batch-001");
        });
    }

    @Test
    void returnsExistingMovementIdWhenReimportIsResolvedByIngestionService() throws Exception {
        var existingMovementId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        when(cashflowIngestionService.ingest(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        existingMovementId,
                        transaction("Venta Caja 1", 125000),
                        new CategoryAssignment(Optional.of(category), false)
                )),
                List.of(),
                List.of()
        ));

        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("Venta Caja 1", "125000", null, "2026-06-15", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorized[0].movementId").value(existingMovementId.toString()));
    }

    @Test
    void returnsSensitiveServiceRejectionWithoutEchoingSensitiveDescriptionOrReference() throws Exception {
        var sensitiveDescription = "Venta Caja RUT 12.345.678-9";
        var sensitiveReference = "paciente 12345";
        when(cashflowIngestionService.ingest(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(),
                List.of(),
                List.of(new CashflowIngestionService.RejectedTransaction(
                        UUID.fromString("66666666-6666-6666-6666-666666666666"),
                        transaction(sensitiveDescription, 42000),
                        "SENSITIVE_IDENTIFIER_REJECTED"
                ))
        ));

        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload(sensitiveDescription, "42000", "CLP", "2026-06-15", sensitiveReference)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejected[0].movementId").value("66666666-6666-6666-6666-666666666666"))
                .andExpect(jsonPath("$.rejected[0].row").value(1))
                .andExpect(jsonPath("$.rejected[0].reasonCode").value("SENSITIVE_IDENTIFIER_REJECTED"))
                .andExpect(jsonPath("$.rejected[0].description").doesNotExist())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(sensitiveDescription))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(sensitiveReference))));
    }

    @Test
    void returnsExactSummaryForTwoAcceptedOneRejectedAndOneInvalidRow() throws Exception {
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        when(cashflowIngestionService.ingest(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        transaction("Venta Caja 1", 125000),
                        new CategoryAssignment(Optional.of(category), false)
                )),
                List.of(),
                List.of(new CashflowIngestionService.RejectedTransaction(
                        UUID.fromString("77777777-7777-7777-7777-777777777777"),
                        transaction("Venta Caja 2", 88000),
                        "SENSITIVE_IDENTIFIER_REJECTED"
                ))
        ));

        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"description": "Venta Caja 1", "amount": 125000, "currency": "CLP", "date": "2026-06-15"},
                                    {"description": "Venta Caja 2", "amount": 88000, "currency": "CLP", "date": "2026-06-15"},
                                    {"description": "Devolución", "amount": -5000, "currency": "CLP", "date": "2026-06-15"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(2))
                .andExpect(jsonPath("$.categorizedCount").value(1))
                .andExpect(jsonPath("$.manualReviewCount").value(0))
                .andExpect(jsonPath("$.rejectedCount").value(1))
                .andExpect(jsonPath("$.invalid").value(1))
                .andExpect(jsonPath("$.categorized[0].movementId").value("11111111-1111-1111-1111-111111111111"))
                .andExpect(jsonPath("$.rejected[0].movementId").value("77777777-7777-7777-7777-777777777777"))
                .andExpect(jsonPath("$.errors[0].row").value(3))
                .andExpect(jsonPath("$.errors[0].field").value("amount"));
    }

    @Test
    void returnsManualReviewMappingWithTransactionAndReason() throws Exception {
        when(cashflowIngestionService.ingest(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(),
                List.of(new CashflowIngestionService.ManualReviewTransaction(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        transaction("Venta Caja 2", 88000),
                        new CategoryAssignment(Optional.empty(), true)
                )),
                List.of()
        ));

        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(singleRowPayload("Venta Caja 2", "88000", "CLP", "2026-06-15", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.manualReviewCount").value(1))
                .andExpect(jsonPath("$.manualReview[0].movementId").value("22222222-2222-2222-2222-222222222222"))
                .andExpect(jsonPath("$.manualReview[0].row").value(1))
                .andExpect(jsonPath("$.manualReview[0].transaction.description").value("Venta Caja 2"))
                .andExpect(jsonPath("$.manualReview[0].transaction.amount").value(88000))
                .andExpect(jsonPath("$.manualReview[0].transaction.currency").value("CLP"))
                .andExpect(jsonPath("$.manualReview[0].transaction.date").value("2026-06-15"))
                .andExpect(jsonPath("$.manualReview[0].reason").value("Requiere clasificación manual."));
    }

    @Test
    void mapsSuccessfulResultsBackToProvidedNonSequentialRowNumbers() throws Exception {
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        when(cashflowIngestionService.ingest(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        transaction("Venta Caja 1", 125000),
                        new CategoryAssignment(Optional.of(category), false)
                )),
                List.of(new CashflowIngestionService.ManualReviewTransaction(
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        transaction("Venta Caja 2", 88000),
                        new CategoryAssignment(Optional.empty(), true)
                )),
                List.of(new CashflowIngestionService.RejectedTransaction(
                        UUID.fromString("77777777-7777-7777-7777-777777777777"),
                        transaction("Venta Caja 3", 44000),
                        "SENSITIVE_IDENTIFIER_REJECTED"
                ))
        ));

        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"rowNumber": 10, "description": "Venta Caja 1", "amount": 125000, "currency": "CLP", "date": "2026-06-15"},
                                    {"rowNumber": 15, "description": "Venta Caja 2", "amount": 88000, "currency": "CLP", "date": "2026-06-15"},
                                    {"rowNumber": 30, "description": "Devolución", "amount": -5000, "currency": "CLP", "date": "2026-06-15"},
                                    {"rowNumber": 99, "description": "Venta Caja 3", "amount": 44000, "currency": "CLP", "date": "2026-06-15"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorized[0].row").value(10))
                .andExpect(jsonPath("$.manualReview[0].row").value(15))
                .andExpect(jsonPath("$.rejected[0].row").value(99))
                .andExpect(jsonPath("$.errors[0].row").value(30));
    }

    @Test
    void fallsBackToSubmittedPositionWhenRowNumberIsMissing() throws Exception {
        var category = new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW);
        when(cashflowIngestionService.ingest(any())).thenReturn(new CashflowIngestionService.CashflowIngestionResult(
                List.of(new CashflowIngestionService.CategorizedTransaction(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        transaction("Venta Caja 1", 125000),
                        new CategoryAssignment(Optional.of(category), false)
                )),
                List.of(),
                List.of()
        ));

        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"description": "Venta Caja 1", "amount": 125000, "currency": "CLP", "date": "2026-06-15"},
                                    {"description": "Devolución", "amount": -5000, "currency": "CLP", "date": "2026-06-15"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorized[0].row").value(1))
                .andExpect(jsonPath("$.errors[0].row").value(2));
    }

    @Test
    void documentsManualImportSuccessAndBadRequestResponsesForOpenApi() throws Exception {
        Method endpoint = CashflowManualImportController.class.getMethod(
                "importManual",
                CashflowManualImportController.ManualImportRequest.class
        );

        var responses = endpoint.getAnnotation(ApiResponses.class).value();

        assertThat(responses)
                .extracting(response -> response.responseCode())
                .contains("200", "400");
    }

    @Test
    void returnsDocumentedResponseShapeWithImportIdListsCountsAndOneBasedRowErrors() throws Exception {
        when(cashflowIngestionService.ingest(any())).thenReturn(emptyIngestionResult());

        mockMvc.perform(post("/api/cashflow/imports/manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "rows": [
                                    {"description": "Venta Caja 1", "amount": 125000, "currency": "CLP", "date": "2026-06-15"},
                                    {"description": "", "amount": 125000, "currency": "CLP", "date": "2026-06-15"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importId").isNotEmpty())
                .andExpect(jsonPath("$.profileId").value("pharmacy-cl"))
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.categorizedCount").value(0))
                .andExpect(jsonPath("$.manualReviewCount").value(0))
                .andExpect(jsonPath("$.rejectedCount").value(0))
                .andExpect(jsonPath("$.invalid").value(1))
                .andExpect(jsonPath("$.categorized").isArray())
                .andExpect(jsonPath("$.manualReview").isArray())
                .andExpect(jsonPath("$.rejected").isArray())
                .andExpect(jsonPath("$.errors[0].row").value(2));
    }

    private static CashflowIngestionService.CashflowIngestionResult emptyIngestionResult() {
        return new CashflowIngestionService.CashflowIngestionResult(List.of(), List.of(), List.of());
    }

    private static String singleRowPayload(String description, String amount, String currency, String date, String externalReference) {
        return """
                {
                  "profileId": "pharmacy-cl",
                  "rows": [
                    {
                      "description": %s,
                      "amount": %s,
                      "currency": %s,
                      "date": %s,
                      "externalReference": %s
                    }
                  ]
                }
                """.formatted(jsonString(description), amount, jsonString(currency), jsonString(date), jsonString(externalReference));
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private static Transaction transaction(String description, int amount) {
        return new Transaction(
                description,
                BigDecimal.valueOf(amount),
                Currency.getInstance("CLP"),
                LocalDate.of(2026, 6, 15)
        );
    }
}
