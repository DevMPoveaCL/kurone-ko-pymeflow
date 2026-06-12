package com.kuroneko.pymeflow.application.port.out;

import com.kuroneko.pymeflow.application.cashflow.CashflowMovementDraft;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementRecord;
import com.kuroneko.pymeflow.application.cashflow.ManualReviewMovementResolutionCommand;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CashflowMovementHistoryPort {
    List<CashflowMovementRecord> saveAll(List<CashflowMovementDraft> drafts);

    Optional<CashflowMovementRecord> findById(UUID movementId);

    List<CashflowMovementRecord> findPendingManualReviews(ProfileId profileId);

    List<CashflowMovementRecord> findProjectionReady(ProfileId profileId);

    Optional<CashflowMovementRecord> resolveManualReview(ManualReviewMovementResolutionCommand command);
}
