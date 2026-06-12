package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.time.LocalDate;
import java.util.List;

public final class CashflowMovementHistoryService {
    private final VerticalProfileService verticalProfileService;
    private final CashflowMovementHistoryPort cashflowMovementHistoryPort;
    private final SensitiveDataPolicy sensitiveDataPolicy;

    public CashflowMovementHistoryService(
            VerticalProfileService verticalProfileService,
            CashflowMovementHistoryPort cashflowMovementHistoryPort
    ) {
        this(verticalProfileService, cashflowMovementHistoryPort, new SensitiveDataPolicy(List.of()));
    }

    public CashflowMovementHistoryService(
            VerticalProfileService verticalProfileService,
            CashflowMovementHistoryPort cashflowMovementHistoryPort,
            SensitiveDataPolicy sensitiveDataPolicy
    ) {
        this.verticalProfileService = verticalProfileService;
        this.cashflowMovementHistoryPort = cashflowMovementHistoryPort;
        this.sensitiveDataPolicy = sensitiveDataPolicy;
    }

    public List<PendingManualReviewMovement> pendingManualReviews(ProfileId profileId) {
        requireProfile(profileId);
        return cashflowMovementHistoryPort.findPendingManualReviews(profileId).stream()
                .filter(record -> record.status() == CashflowMovementStatus.MANUAL_REVIEW)
                .map(PendingManualReviewMovement::from)
                .toList();
    }

    public List<ProjectionReadyCashflowTransaction> projectionReady(
            ProfileId profileId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        requireProfile(profileId);
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial.");
        }
        return cashflowMovementHistoryPort.findProjectionReady(profileId).stream()
                .filter(record -> record.status() == CashflowMovementStatus.PROJECTABLE)
                .filter(record -> startDate == null || !record.date().isBefore(startDate))
                .filter(record -> endDate == null || !record.date().isAfter(endDate))
                .map(ProjectionReadyCashflowTransaction::from)
                .toList();
    }

    public PersistedManualReviewResolutionResult resolveManualReview(ManualReviewMovementResolutionCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("La solicitud de resolución es obligatoria.");
        }
        var profile = requireProfile(command.profileId());
        validateSafeText(command.description());
        validateSafeText(command.sourceReference());
        var category = profile.categories().stream()
                .filter(candidate -> candidate.key().equals(command.categoryKey()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La categoría enviada no está configurada para el perfil."));
        var movement = cashflowMovementHistoryPort.findById(command.movementId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontró el movimiento solicitado."));
        if (!movement.profileId().equals(command.profileId())) {
            throw new IllegalArgumentException("No se encontró el movimiento solicitado.");
        }
        if (movement.status() != CashflowMovementStatus.MANUAL_REVIEW) {
            throw new IllegalArgumentException("El movimiento ya fue resuelto o no está disponible para revisión manual.");
        }

        var resolved = cashflowMovementHistoryPort.resolveManualReview(command)
                .orElseThrow(() -> new IllegalArgumentException("El movimiento ya fue resuelto o no está disponible para revisión manual."));
        return new PersistedManualReviewResolutionResult(
                ProjectionReadyCashflowTransaction.from(resolved),
                category,
                resolved.safeDescriptionOptional(),
                resolved.sourceReferenceOptional()
        );
    }

    private com.kuroneko.pymeflow.domain.vertical.VerticalProfile requireProfile(ProfileId profileId) {
        if (profileId == null) {
            throw new IllegalArgumentException("El perfil es obligatorio.");
        }
        try {
            return verticalProfileService.loadProfile(profileId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El perfil indicado no está configurado.", exception);
        }
    }

    private void validateSafeText(String text) {
        if (sensitiveDataPolicy.rejectsText(text)) {
            throw new IllegalArgumentException("La información enviada contiene datos sensibles y no puede proyectarse.");
        }
    }
}
