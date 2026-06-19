package com.kuroneko.pymeflow.application.recommendation;

import com.kuroneko.pymeflow.application.cashflow.CashflowMovementDraft;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementRecord;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementStatus;
import com.kuroneko.pymeflow.application.cashflow.ManualReviewMovementResolutionCommand;
import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HistoryRecommendationServiceTest {
    private static final ProfileId PROFILE_ID = new ProfileId("pharmacy-cl");
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-18T12:00:00Z"), ZoneOffset.UTC);
    private static final Currency CLP = Currency.getInstance("CLP");

    @Test
    void generatesWarningWhenManualReviewBacklogExceedsThreshold() {
        var history = new FakeCashflowMovementHistoryPort()
                .withManualReviews(6)
                .withProjectable(projectable("sales", 1_000, LocalDate.of(2026, 6, 10)), 10);
        var service = service(history);

        var response = service.generate(PROFILE_ID);

        assertThat(response.profileId()).isEqualTo("pharmacy-cl");
        assertThat(response.generatedAt()).isEqualTo(FIXED_CLOCK.instant());
        assertThat(response.signals()).anySatisfy(signal -> {
            assertThat(signal.type()).isEqualTo("MANUAL_REVIEW_BACKLOG");
            assertThat(signal.severity()).isEqualTo("WARNING");
            assertThat(signal.title()).isEqualTo("Revisión manual pendiente");
            assertThat(signal.actionHint()).isEqualTo("Revisa y categoriza estos movimientos.");
            assertThat(signal.metrics()).containsEntry("pendingCount", 6);
        });
    }

    @Test
    void generatesWarningWhenManualReviewBacklogReachesThreshold() {
        var response = service(new FakeCashflowMovementHistoryPort()
                .withManualReviews(5)
                .withProjectable(projectable("sales", 1_000, LocalDate.of(2026, 6, 10)), 10))
                .generate(PROFILE_ID);

        assertThat(signal(response, "MANUAL_REVIEW_BACKLOG").severity()).isEqualTo("WARNING");
        assertThat(signal(response, "MANUAL_REVIEW_BACKLOG").metrics()).containsEntry("pendingCount", 5);
    }

    @Test
    void generatesInfoForSmallManualReviewBacklogAndOmitsSignalWhenEmpty() {
        var smallBacklogResponse = service(new FakeCashflowMovementHistoryPort()
                .withManualReviews(4)
                .withProjectable(projectable("sales", 1_000, LocalDate.of(2026, 6, 10)), 10))
                .generate(PROFILE_ID);

        assertThat(signalTypes(smallBacklogResponse)).contains("MANUAL_REVIEW_BACKLOG");
        assertThat(signal(smallBacklogResponse, "MANUAL_REVIEW_BACKLOG").severity()).isEqualTo("INFO");
        assertThat(signal(smallBacklogResponse, "MANUAL_REVIEW_BACKLOG").metrics()).containsEntry("pendingCount", 4);

        var emptyBacklogResponse = service(new FakeCashflowMovementHistoryPort()
                .withProjectable(projectable("sales", 1_000, LocalDate.of(2026, 6, 10)), 10))
                .generate(PROFILE_ID);

        assertThat(signalTypes(emptyBacklogResponse)).doesNotContain("MANUAL_REVIEW_BACKLOG");
    }

    @Test
    void generatesHighRejectionRateWarningWhenRejectedRowsReachThirtyPercentOfTotalHistory() {
        var history = new FakeCashflowMovementHistoryPort()
                .withProjectable(projectable("sales", 1_000, LocalDate.of(2026, 6, 10)), 5)
                .withManualReviews(2)
                .withRejected(3, "INVALID_AMOUNT", null, null);

        var response = service(history).generate(PROFILE_ID);

        var signal = signal(response, "HIGH_REJECTION_RATE");
        assertThat(signal.severity()).isEqualTo("WARNING");
        assertThat(signal.metrics()).containsEntry("rejectedCount", 3)
                .containsEntry("projectableCount", 5)
                .containsEntry("rejectionRatePercent", BigDecimal.valueOf(30).setScale(2));
    }

    @Test
    void generatesHighRejectionRateWarningUsingAggregateMetricsOnly() {
        var history = new FakeCashflowMovementHistoryPort()
                .withProjectable(projectable("sales", 1_000, LocalDate.of(2026, 6, 10)), 10)
                .withRejected(5, "INVALID_AMOUNT", "Sensitive client row", "bank-file-line-42");

        var response = service(history).generate(PROFILE_ID);

        var signal = signal(response, "HIGH_REJECTION_RATE");
        assertThat(signal.severity()).isEqualTo("WARNING");
        assertThat(signal.title()).isEqualTo("Alta tasa de rechazo");
        assertThat(signal.actionHint()).isEqualTo("Revisa los motivos de rechazo para corregir la fuente de datos.");
        assertThat(signal.metrics()).containsOnly(
                Map.entry("rejectedCount", 5),
                Map.entry("projectableCount", 10),
                Map.entry("rejectionRatePercent", BigDecimal.valueOf(33.33)),
                Map.entry("topRejectionReasonCode", "INVALID_AMOUNT")
        );
        assertThat(signal.toString()).doesNotContain("Sensitive client row", "bank-file-line-42", "safeDescription", "sourceReference");
    }

    @Test
    void omitsHighRejectionRateWhenThresholdIsNotExceeded() {
        var history = new FakeCashflowMovementHistoryPort()
                .withProjectable(projectable("sales", 1_000, LocalDate.of(2026, 6, 10)), 10)
                .withRejected(2, "INVALID_AMOUNT", null, null);

        var response = service(history).generate(PROFILE_ID);

        assertThat(signalTypes(response)).doesNotContain("HIGH_REJECTION_RATE");
    }

    @Test
    void generatesCategoryConcentrationInfoWhenOneCategoryReachesProjectableAmountThreshold() {
        var history = new FakeCashflowMovementHistoryPort()
                .withProjectable(projectable("sales", 100, LocalDate.of(2026, 6, 10)), 6)
                .withProjectable(projectable("supplies", 100, LocalDate.of(2026, 6, 11)), 4);

        var response = service(history).generate(PROFILE_ID);

        assertThat(signal(response, "CATEGORY_CONCENTRATION").metrics())
                .containsEntry("categoryKey", "sales")
                .containsEntry("concentrationPercent", BigDecimal.valueOf(60).setScale(2));
    }

    @Test
    void generatesCategoryConcentrationInfoWhenOneCategoryExceedsProjectableAmountThreshold() {
        var history = new FakeCashflowMovementHistoryPort()
                .withProjectable(projectable("sales", 700, LocalDate.of(2026, 6, 10)), 1)
                .withProjectable(projectable("supplies", 300, LocalDate.of(2026, 6, 11)), 1)
                .withProjectable(projectable("other", 1, LocalDate.of(2026, 6, 12)), 8);

        var response = service(history).generate(PROFILE_ID);

        var signal = signal(response, "CATEGORY_CONCENTRATION");

        assertThat(signal.severity()).isEqualTo("INFO");
        assertThat(signal.title()).isEqualTo("Concentración por categoría");
        assertThat(signal.metrics()).containsEntry("categoryKey", "sales")
                .containsEntry("concentrationPercent", BigDecimal.valueOf(69.44));
    }

    @Test
    void reportsInsufficientDataForEmptyHistoryAndNoProjectableMovements() {
        var emptyResponse = service(new FakeCashflowMovementHistoryPort()).generate(PROFILE_ID);

        assertThat(signalTypes(emptyResponse)).containsExactly("INSUFFICIENT_DATA");
        assertThat(signal(emptyResponse, "INSUFFICIENT_DATA").metrics()).containsEntry("projectableCount", 0);

        var noProjectableResponse = service(new FakeCashflowMovementHistoryPort()
                .withManualReviews(2)
                .withRejected(1, "INVALID_AMOUNT", null, null))
                .generate(PROFILE_ID);

        assertThat(signalTypes(noProjectableResponse)).contains("INSUFFICIENT_DATA");
        assertThat(signal(noProjectableResponse, "INSUFFICIENT_DATA").metrics()).containsEntry("projectableCount", 0);
    }

    @Test
    void generatesRecentInactivityWarningWhenNoMovementWasRecordedInLastThirtyDays() {
        var response = service(new FakeCashflowMovementHistoryPort()
                .withProjectable(projectable("sales", 1_000, LocalDate.of(2026, 5, 1)), 10))
                .generate(PROFILE_ID);

        var signal = signal(response, "RECENT_INACTIVITY");

        assertThat(signal.severity()).isEqualTo("WARNING");
        assertThat(signal.title()).isEqualTo("Inactividad reciente");
        assertThat(signal.metrics()).containsEntry("daysSinceLastMovement", 48L);
    }

    @Test
    void generatesHealthyHistoryOnlyWhenNoOtherSignalApplies() {
        var response = service(new FakeCashflowMovementHistoryPort()
                .withProjectable(projectable("sales", 300, LocalDate.of(2026, 6, 10)), 5)
                .withProjectable(projectable("supplies", 300, LocalDate.of(2026, 6, 11)), 5))
                .generate(PROFILE_ID);

        assertThat(signalTypes(response)).containsExactly("HEALTHY_HISTORY");
        assertThat(signal(response, "HEALTHY_HISTORY").title()).isEqualTo("Historial saludable");
    }

    @Test
    void ordersWarningsBeforeInformationWithStableOrderInsideSeverity() {
        var response = service(new FakeCashflowMovementHistoryPort()
                .withManualReviews(6, LocalDate.of(2026, 5, 1))
                .withProjectable(projectable("sales", 700, LocalDate.of(2026, 5, 1)), 7)
                .withProjectable(projectable("supplies", 300, LocalDate.of(2026, 5, 2)), 3)
                .withRejected(10, "INVALID_AMOUNT", null, null, LocalDate.of(2026, 5, 1)))
                .generate(PROFILE_ID);

        assertThat(signalTypes(response)).containsExactly(
                "MANUAL_REVIEW_BACKLOG",
                "HIGH_REJECTION_RATE",
                "RECENT_INACTIVITY",
                "CATEGORY_CONCENTRATION"
        );
    }

    @Test
    void propagatesProfileNotFoundWithoutLoadingHistory() {
        var history = new FakeCashflowMovementHistoryPort();
        var profileService = new VerticalProfileService(id -> Optional.empty());
        var service = new HistoryRecommendationService(profileService, history, FIXED_CLOCK);

        assertThatThrownBy(() -> service.generate(PROFILE_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile not found: pharmacy-cl");
        assertThat(history.queriedStatuses).isEmpty();
    }

    @Test
    void recomputesDeterministicallyWithoutPersistingSnapshotsAcrossRepeatedRequests() {
        var history = new FakeCashflowMovementHistoryPort()
                .withProjectable(projectable("sales", 300, LocalDate.of(2026, 6, 10)), 5)
                .withProjectable(projectable("supplies", 300, LocalDate.of(2026, 6, 11)), 5);
        var service = service(history);

        var firstResponse = service.generate(PROFILE_ID);
        var secondResponse = service.generate(PROFILE_ID);

        assertThat(secondResponse).isEqualTo(firstResponse);
        assertThat(history.queriedStatuses).containsExactly(
                CashflowMovementStatus.MANUAL_REVIEW,
                CashflowMovementStatus.PROJECTABLE,
                CashflowMovementStatus.REJECTED,
                CashflowMovementStatus.MANUAL_REVIEW,
                CashflowMovementStatus.PROJECTABLE,
                CashflowMovementStatus.REJECTED
        );
        assertThat(history.savedDraftBatches).isZero();
        assertThat(history.resolvedManualReviews).isZero();
    }

    private static HistoryRecommendationService service(FakeCashflowMovementHistoryPort historyPort) {
        return new HistoryRecommendationService(
                new VerticalProfileService(id -> Optional.of(profile())),
                historyPort,
                FIXED_CLOCK
        );
    }

    private static List<String> signalTypes(HistoryRecommendationService.HistoryRecommendationResponse response) {
        return response.signals().stream()
                .map(HistoryRecommendationService.HistorySignalResponse::type)
                .toList();
    }

    private static HistoryRecommendationService.HistorySignalResponse signal(
            HistoryRecommendationService.HistoryRecommendationResponse response,
            String type
    ) {
        return response.signals().stream()
                .filter(signal -> signal.type().equals(type))
                .findFirst()
                .orElseThrow();
    }

    private static VerticalProfile profile() {
        return new VerticalProfile(
                PROFILE_ID,
                "Farmacia",
                List.of(),
                List.of(new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW)),
                List.of()
        );
    }

    private static CashflowMovementRecord projectable(String categoryKey, int amount, LocalDate date) {
        return movement(CashflowMovementStatus.PROJECTABLE, categoryKey, amount, date, "Ingreso registrado", "bank-file", null);
    }

    private static CashflowMovementRecord movement(
            CashflowMovementStatus status,
            String categoryKey,
            int amount,
            LocalDate date,
            String safeDescription,
            String sourceReference,
            String rejectionReasonCode
    ) {
        var now = FIXED_CLOCK.instant();
        return new CashflowMovementRecord(
                UUID.randomUUID(),
                PROFILE_ID,
                BigDecimal.valueOf(amount),
                CLP,
                date,
                status,
                categoryKey,
                safeDescription,
                sourceReference,
                rejectionReasonCode,
                null,
                now,
                now
        );
    }

    private static final class FakeCashflowMovementHistoryPort implements CashflowMovementHistoryPort {
        private final List<CashflowMovementRecord> projectable = new ArrayList<>();
        private final List<CashflowMovementRecord> manualReview = new ArrayList<>();
        private final List<CashflowMovementRecord> rejected = new ArrayList<>();
        private final List<CashflowMovementStatus> queriedStatuses = new ArrayList<>();
        private int savedDraftBatches;
        private int resolvedManualReviews;

        private FakeCashflowMovementHistoryPort withProjectable(CashflowMovementRecord movement, int times) {
            for (int i = 0; i < times; i++) {
                projectable.add(movement);
            }
            return this;
        }

        private FakeCashflowMovementHistoryPort withManualReviews(int count) {
            return withManualReviews(count, LocalDate.of(2026, 6, 10));
        }

        private FakeCashflowMovementHistoryPort withManualReviews(int count, LocalDate date) {
            for (int i = 0; i < count; i++) {
                manualReview.add(movement(CashflowMovementStatus.MANUAL_REVIEW, null, 1_000, date, "Pendiente", "manual", null));
            }
            return this;
        }

        private FakeCashflowMovementHistoryPort withRejected(int count, String reasonCode, String safeDescription, String sourceReference) {
            return withRejected(count, reasonCode, safeDescription, sourceReference, LocalDate.of(2026, 6, 10));
        }

        private FakeCashflowMovementHistoryPort withRejected(
                int count,
                String reasonCode,
                String safeDescription,
                String sourceReference,
                LocalDate date
        ) {
            for (int i = 0; i < count; i++) {
                rejected.add(movement(CashflowMovementStatus.REJECTED, null, 1_000, date, safeDescription, sourceReference, reasonCode));
            }
            return this;
        }

        @Override
        public List<CashflowMovementRecord> saveAll(List<CashflowMovementDraft> drafts) {
            savedDraftBatches++;
            return List.of();
        }

        @Override
        public Optional<CashflowMovementRecord> findById(UUID movementId) {
            throw new UnsupportedOperationException("Not needed by recommendation tests");
        }

        @Override
        public Optional<CashflowMovementRecord> findBySourceReference(ProfileId profileId, String sourceReference) {
            throw new UnsupportedOperationException("Not needed by recommendation tests");
        }

        @Override
        public List<CashflowMovementRecord> findByStatus(ProfileId profileId, CashflowMovementStatus status) {
            queriedStatuses.add(status);
            return switch (status) {
                case PROJECTABLE -> List.copyOf(projectable);
                case MANUAL_REVIEW -> List.copyOf(manualReview);
                case REJECTED -> List.copyOf(rejected);
            };
        }

        @Override
        public List<CashflowMovementRecord> findPendingManualReviews(ProfileId profileId) {
            throw new UnsupportedOperationException("Not needed by recommendation tests");
        }

        @Override
        public List<CashflowMovementRecord> findProjectionReady(ProfileId profileId) {
            throw new UnsupportedOperationException("Not needed by recommendation tests");
        }

        @Override
        public Optional<CashflowMovementRecord> resolveManualReview(ManualReviewMovementResolutionCommand command) {
            resolvedManualReviews++;
            return Optional.empty();
        }
    }
}
