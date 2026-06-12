package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

public final class ManualReviewResolutionService {
    public static final String SOURCE_STATUS_MANUAL_REVIEW = "MANUAL_REVIEW";
    public static final String STATUS_PROJECTABLE = "PROJECTABLE";
    public static final String STATUS_CATEGORIZED = "CATEGORIZED";
    public static final String STATUS_REJECTED = "REJECTED";

    private final VerticalProfileService verticalProfileService;
    private final SensitiveDataPolicy sensitiveDataPolicy;

    public ManualReviewResolutionService(
            VerticalProfileService verticalProfileService,
            SensitiveDataPolicy sensitiveDataPolicy
    ) {
        this.verticalProfileService = verticalProfileService;
        this.sensitiveDataPolicy = sensitiveDataPolicy;
    }

    public ManualReviewResolutionResult resolve(ManualReviewResolutionCommand command) {
        requireCommand(command);
        validateFinancialFields(command);
        validateSourceStatus(command.sourceStatus());
        var outputStatus = validateOutputStatus(command.outputStatus());
        validateSafeText(command.description(), "La descripción contiene información sensible y no puede proyectarse.");
        validateSafeText(command.sourceReference(), "La referencia de origen contiene información sensible y no puede proyectarse.");

        var profile = loadProfile(command);
        var category = profile.categories().stream()
                .filter(candidate -> candidate.key().equals(command.categoryKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La categoría seleccionada no existe para el perfil indicado."));

        return new ManualReviewResolutionResult(
                new ProjectedCashflowTransaction(
                        category.key(),
                        command.amount(),
                        command.currency(),
                        command.date()
                ),
                category,
                safeOptional(command.description()),
                safeOptional(command.sourceReference()),
                outputStatus
        );
    }

    private static void requireCommand(ManualReviewResolutionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("La solicitud de resolución es obligatoria.");
        }
        if (command.profileId() == null) {
            throw new IllegalArgumentException("El perfil es obligatorio.");
        }
        if (command.categoryKey() == null || command.categoryKey().isBlank()) {
            throw new IllegalArgumentException("La categoría es obligatoria.");
        }
    }

    private static void validateFinancialFields(ManualReviewResolutionCommand command) {
        if (command.amount() == null || command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero.");
        }
        if (command.currency() == null) {
            throw new IllegalArgumentException("La moneda es obligatoria.");
        }
        if (command.date() == null) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }
    }

    private static void validateSourceStatus(String sourceStatus) {
        var normalizedStatus = normalize(sourceStatus);
        if (normalizedStatus.isEmpty()) {
            return;
        }
        if (STATUS_REJECTED.equals(normalizedStatus.get())) {
            throw new IllegalArgumentException("Un movimiento rechazado no puede convertirse en proyección.");
        }
        if (!SOURCE_STATUS_MANUAL_REVIEW.equals(normalizedStatus.get())) {
            throw new IllegalArgumentException("Solo se pueden resolver movimientos enviados a revisión manual.");
        }
    }

    private static String validateOutputStatus(String outputStatus) {
        var normalizedStatus = normalize(outputStatus).orElse(STATUS_PROJECTABLE);
        if (SOURCE_STATUS_MANUAL_REVIEW.equals(normalizedStatus) || STATUS_REJECTED.equals(normalizedStatus)) {
            throw new IllegalArgumentException("El resultado de la resolución debe quedar listo para proyección.");
        }
        if (!STATUS_PROJECTABLE.equals(normalizedStatus) && !STATUS_CATEGORIZED.equals(normalizedStatus)) {
            throw new IllegalArgumentException("El estado de salida no es válido para proyección.");
        }
        return normalizedStatus;
    }

    private void validateSafeText(String text, String message) {
        if (sensitiveDataPolicy.rejectsText(text)) {
            throw new IllegalArgumentException(message);
        }
    }

    private com.kuroneko.pymeflow.domain.vertical.VerticalProfile loadProfile(ManualReviewResolutionCommand command) {
        try {
            return verticalProfileService.loadProfile(command.profileId());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El perfil indicado no está configurado.", exception);
        }
    }

    private static Optional<String> safeOptional(String text) {
        return Optional.ofNullable(text)
                .map(String::trim)
                .filter(value -> !value.isBlank());
    }

    private static Optional<String> normalize(String status) {
        return safeOptional(status).map(value -> value.toUpperCase(Locale.ROOT));
    }
}
