package com.kuroneko.pymeflow.application.cashflow;

import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.time.LocalDate;
import java.util.List;

public final class CashflowMovementHistoryService {
    private final VerticalProfileService verticalProfileService;
    private final CashflowMovementHistoryPort cashflowMovementHistoryPort;

    public CashflowMovementHistoryService(
            VerticalProfileService verticalProfileService,
            CashflowMovementHistoryPort cashflowMovementHistoryPort
    ) {
        this.verticalProfileService = verticalProfileService;
        this.cashflowMovementHistoryPort = cashflowMovementHistoryPort;
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

    private void requireProfile(ProfileId profileId) {
        if (profileId == null) {
            throw new IllegalArgumentException("El perfil es obligatorio.");
        }
        try {
            verticalProfileService.loadProfile(profileId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("El perfil indicado no está configurado.", exception);
        }
    }
}
