package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManualReviewResolutionServiceTest {
    private static final Currency CLP = Currency.getInstance("CLP");
    private static final ProfileId PROFILE_ID = new ProfileId("retail-cl");

    @Test
    void resolvesManualReviewMovementIntoProjectableTransaction() {
        var service = service(profile());

        var result = service.resolve(command("sales", "Venta Caja 1", "caja-1", "MANUAL_REVIEW", null));

        assertThat(result.transaction()).isEqualTo(new ProjectedCashflowTransaction(
                "sales",
                BigDecimal.valueOf(10_000),
                CLP,
                LocalDate.of(2026, 1, 10)
        ));
        assertThat(result.category().displayName()).isEqualTo("Sales");
        assertThat(result.safeDescription()).contains("Venta Caja 1");
        assertThat(result.safeSourceReference()).contains("caja-1");
        assertThat(result.outputStatus()).isEqualTo(ManualReviewResolutionService.STATUS_PROJECTABLE);
    }

    @Test
    void resolvesMinimalRequestWithoutDescriptionOrReference() {
        var service = service(profile());

        var result = service.resolve(command("sales", null, null, null, "CATEGORIZED"));

        assertThat(result.transaction().categoryKey()).isEqualTo("sales");
        assertThat(result.safeDescription()).isEmpty();
        assertThat(result.safeSourceReference()).isEmpty();
        assertThat(result.outputStatus()).isEqualTo(ManualReviewResolutionService.STATUS_CATEGORIZED);
    }

    @Test
    void treatsRepeatedResolutionAsStatelessRequest() {
        var service = service(profile());
        var request = command("sales", "Venta Caja 1", null, "MANUAL_REVIEW", null);

        var first = service.resolve(request);
        var second = service.resolve(request);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void rejectsUnknownProfileOrCategory() {
        var missingProfileService = new ManualReviewResolutionService(
                new VerticalProfileService(id -> Optional.empty()),
                new SensitiveDataPolicy(List.of())
        );

        assertThatThrownBy(() -> missingProfileService.resolve(command("sales", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("perfil indicado no está configurado");

        assertThatThrownBy(() -> service(profile()).resolve(command("unknown", null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("categoría seleccionada no existe");
    }

    @Test
    void rejectsSensitiveDescriptionOrSourceReference() {
        var service = new ManualReviewResolutionService(
                new VerticalProfileService(id -> Optional.of(profile())),
                new SensitiveDataPolicy(List.of("blocked-token"))
        );

        assertThatThrownBy(() -> service.resolve(command("sales", "blocked-token", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descripción contiene información sensible");

        assertThatThrownBy(() -> service.resolve(command("sales", null, "blocked-token", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("referencia de origen contiene información sensible");
    }

    @Test
    void rejectsRejectedSourceAndManualReviewOrRejectedOutputStatus() {
        var service = service(profile());

        assertThatThrownBy(() -> service.resolve(command("sales", null, null, "REJECTED", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("movimiento rechazado no puede convertirse");

        assertThatThrownBy(() -> service.resolve(command("sales", null, null, null, "MANUAL_REVIEW")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debe quedar listo para proyección");

        assertThatThrownBy(() -> service.resolve(command("sales", null, null, null, "REJECTED")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debe quedar listo para proyección");
    }

    @Test
    void rejectsInvalidFinancialFields() {
        var service = service(profile());

        assertThatThrownBy(() -> service.resolve(new ManualReviewResolutionCommand(
                PROFILE_ID,
                "sales",
                BigDecimal.ZERO,
                CLP,
                LocalDate.of(2026, 1, 10),
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monto debe ser mayor a cero");

        assertThatThrownBy(() -> service.resolve(new ManualReviewResolutionCommand(
                PROFILE_ID,
                "sales",
                BigDecimal.ONE,
                null,
                LocalDate.of(2026, 1, 10),
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moneda es obligatoria");
    }

    private static ManualReviewResolutionService service(VerticalProfile profile) {
        return new ManualReviewResolutionService(
                new VerticalProfileService(id -> Optional.of(profile)),
                new SensitiveDataPolicy(List.of())
        );
    }

    private static ManualReviewResolutionCommand command(
            String categoryKey,
            String description,
            String sourceReference,
            String sourceStatus,
            String outputStatus
    ) {
        return new ManualReviewResolutionCommand(
                PROFILE_ID,
                categoryKey,
                BigDecimal.valueOf(10_000),
                CLP,
                LocalDate.of(2026, 1, 10),
                description,
                sourceReference,
                sourceStatus,
                outputStatus
        );
    }

    private static VerticalProfile profile() {
        return new VerticalProfile(
                PROFILE_ID,
                "Retail",
                List.of(),
                List.of(
                        new CashflowCategory("sales", "Sales", CashflowDirection.INFLOW),
                        new CashflowCategory("suppliers", "Suppliers", CashflowDirection.OUTFLOW)
                ),
                List.of()
        );
    }
}
