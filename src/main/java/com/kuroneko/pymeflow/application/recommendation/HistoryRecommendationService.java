package com.kuroneko.pymeflow.application.recommendation;

import com.kuroneko.pymeflow.application.cashflow.CashflowMovementRecord;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementStatus;
import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class HistoryRecommendationService {
    private static final int MANUAL_REVIEW_WARNING_THRESHOLD = 5;
    private static final int MINIMUM_PROJECTABLE_MOVEMENTS = 10;
    private static final BigDecimal REJECTION_RATE_WARNING_THRESHOLD = BigDecimal.valueOf(30);
    private static final BigDecimal CATEGORY_CONCENTRATION_THRESHOLD = BigDecimal.valueOf(60);
    private static final int RECENT_ACTIVITY_DAYS = 30;

    private final VerticalProfileService verticalProfileService;
    private final CashflowMovementHistoryPort historyPort;
    private final Clock clock;

    public HistoryRecommendationService(
            VerticalProfileService verticalProfileService,
            CashflowMovementHistoryPort historyPort
    ) {
        this(verticalProfileService, historyPort, Clock.systemUTC());
    }

    public HistoryRecommendationService(
            VerticalProfileService verticalProfileService,
            CashflowMovementHistoryPort historyPort,
            Clock clock
    ) {
        this.verticalProfileService = Objects.requireNonNull(verticalProfileService, "Vertical profile service is required");
        this.historyPort = Objects.requireNonNull(historyPort, "Cashflow movement history port is required");
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    public HistoryRecommendationResponse generate(ProfileId profileId) {
        var profile = verticalProfileService.loadProfile(profileId);

        var manualReview = historyPort.findByStatus(profileId, CashflowMovementStatus.MANUAL_REVIEW);
        var projectable = historyPort.findByStatus(profileId, CashflowMovementStatus.PROJECTABLE);
        var rejected = historyPort.findByStatus(profileId, CashflowMovementStatus.REJECTED);

        var signals = new ArrayList<HistorySignalResponse>();
        addManualReviewBacklogSignal(signals, manualReview);
        addHighRejectionRateSignal(signals, manualReview, projectable, rejected);
        addCategoryConcentrationSignal(signals, projectable);
        addInsufficientDataSignal(signals, projectable);
        addRecentInactivitySignal(signals, manualReview, projectable, rejected);
        addDirectionMismatchSignal(signals, projectable, profile.categories());

        if (signals.isEmpty()) {
            signals.add(new HistorySignalResponse(
                    "HEALTHY_HISTORY",
                    "INFO",
                    "Historial saludable",
                    "Tu historial tiene datos suficientes y sin señales relevantes de alerta.",
                    "Sigue registrando movimientos para mantener esta tendencia.",
                    Map.of("projectableCount", projectable.size())
            ));
        }

        var orderedSignals = signals.stream()
                .sorted(Comparator.comparingInt(HistoryRecommendationService::severityRank))
                .toList();

        return new HistoryRecommendationResponse(profileId.value(), Instant.now(clock), orderedSignals);
    }

    private static void addManualReviewBacklogSignal(
            List<HistorySignalResponse> signals,
            List<CashflowMovementRecord> manualReview
    ) {
        var pendingCount = manualReview.size();
        if (pendingCount == 0) {
            return;
        }

        signals.add(new HistorySignalResponse(
                "MANUAL_REVIEW_BACKLOG",
                pendingCount >= MANUAL_REVIEW_WARNING_THRESHOLD ? "WARNING" : "INFO",
                "Revisión manual pendiente",
                "Hay movimientos pendientes de revisión manual antes de proyectarlos.",
                "Revisa y categoriza estos movimientos.",
                Map.of("pendingCount", pendingCount)
        ));
    }

    private static void addHighRejectionRateSignal(
            List<HistorySignalResponse> signals,
            List<CashflowMovementRecord> manualReview,
            List<CashflowMovementRecord> projectable,
            List<CashflowMovementRecord> rejected
    ) {
        var total = manualReview.size() + projectable.size() + rejected.size();
        if (total == 0 || rejected.isEmpty()) {
            return;
        }

        var rejectionRate = percent(BigDecimal.valueOf(rejected.size()), BigDecimal.valueOf(total));
        if (rejectionRate.compareTo(REJECTION_RATE_WARNING_THRESHOLD) < 0) {
            return;
        }

        signals.add(new HistorySignalResponse(
                "HIGH_REJECTION_RATE",
                "WARNING",
                "Alta tasa de rechazo",
                "Una proporción relevante del historial fue rechazada por datos inválidos o sensibles.",
                "Revisa los motivos de rechazo para corregir la fuente de datos.",
                Map.of(
                        "rejectedCount", rejected.size(),
                        "projectableCount", projectable.size(),
                        "rejectionRatePercent", rejectionRate,
                        "topRejectionReasonCode", topRejectionReasonCode(rejected).orElse("UNSPECIFIED")
                )
        ));
    }

    private static void addCategoryConcentrationSignal(
            List<HistorySignalResponse> signals,
            List<CashflowMovementRecord> projectable
    ) {
        var amountByCategory = projectable.stream()
                .filter(movement -> movement.categoryKey() != null && !movement.categoryKey().isBlank())
                .collect(Collectors.groupingBy(
                        CashflowMovementRecord::categoryKey,
                        Collectors.reducing(BigDecimal.ZERO, CashflowMovementRecord::amount, BigDecimal::add)
                ));
        var totalAmount = amountByCategory.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalAmount.signum() == 0) {
            return;
        }

        amountByCategory.entrySet().stream()
                .map(entry -> new CategoryConcentration(entry.getKey(), entry.getValue(), percent(entry.getValue(), totalAmount)))
                .filter(concentration -> concentration.percent().compareTo(CATEGORY_CONCENTRATION_THRESHOLD) >= 0)
                .sorted(Comparator.comparing(CategoryConcentration::amount).reversed()
                        .thenComparing(CategoryConcentration::categoryKey))
                .findFirst()
                .ifPresent(concentration -> signals.add(new HistorySignalResponse(
                        "CATEGORY_CONCENTRATION",
                        "INFO",
                        "Concentración por categoría",
                        "Una categoría concentra una parte importante del monto proyectable.",
                        "Revisa si la distribución de categorías refleja tu operación real.",
                        Map.of(
                                "categoryKey", concentration.categoryKey(),
                                "concentrationPercent", concentration.percent()
                        )
                )));
    }

    private static void addInsufficientDataSignal(
            List<HistorySignalResponse> signals,
            List<CashflowMovementRecord> projectable
    ) {
        if (projectable.size() >= MINIMUM_PROJECTABLE_MOVEMENTS) {
            return;
        }

        signals.add(new HistorySignalResponse(
                "INSUFFICIENT_DATA",
                "INFO",
                "Datos insuficientes",
                "El historial proyectable aún no alcanza el volumen mínimo para señales más robustas.",
                "Agrega más movimientos para obtener mejores recomendaciones.",
                Map.of(
                        "projectableCount", projectable.size(),
                        "minimumProjectableCount", MINIMUM_PROJECTABLE_MOVEMENTS
                )
        ));
    }

    private void addRecentInactivitySignal(
            List<HistorySignalResponse> signals,
            List<CashflowMovementRecord> manualReview,
            List<CashflowMovementRecord> projectable,
            List<CashflowMovementRecord> rejected
    ) {
        var lastMovementDate = allMovements(manualReview, projectable, rejected).stream()
                .map(CashflowMovementRecord::date)
                .max(LocalDate::compareTo);

        if (lastMovementDate.isEmpty()) {
            return;
        }

        var today = LocalDate.now(clock);
        var daysSinceLastMovement = ChronoUnit.DAYS.between(lastMovementDate.get(), today);
        if (daysSinceLastMovement <= RECENT_ACTIVITY_DAYS) {
            return;
        }

        signals.add(new HistorySignalResponse(
                "RECENT_INACTIVITY",
                "WARNING",
                "Inactividad reciente",
                "No se registran movimientos recientes en el historial.",
                "Registra los movimientos más recientes para mantener el historial actualizado.",
                Map.of("daysSinceLastMovement", daysSinceLastMovement)
        ));
    }

    private static void addDirectionMismatchSignal(
            List<HistorySignalResponse> signals,
            List<CashflowMovementRecord> projectable,
            List<CashflowCategory> categories
    ) {
        var directionsByCategory = categories.stream()
                .collect(Collectors.toMap(CashflowCategory::key, CashflowCategory::direction));

        var debitInflowCount = projectable.stream()
                .filter(movement -> movement.direction() == TransactionDirection.DEBIT)
                .filter(movement -> directionsByCategory.get(movement.categoryKey()) == CashflowDirection.INFLOW)
                .count();
        var creditOutflowCount = projectable.stream()
                .filter(movement -> movement.direction() == TransactionDirection.CREDIT)
                .filter(movement -> directionsByCategory.get(movement.categoryKey()) == CashflowDirection.OUTFLOW)
                .count();

        if (debitInflowCount == 0 && creditOutflowCount == 0) {
            return;
        }

        signals.add(new HistorySignalResponse(
                "DIRECTION_MISMATCH",
                "INFO",
                "Diferencia entre movimiento y categoría",
                "Algunos movimientos tienen una dirección bancaria distinta a la dirección de su categoría.",
                "Revisa si la categoría asignada refleja correctamente el movimiento bancario.",
                Map.of(
                        "debitInflowCount", debitInflowCount,
                        "creditOutflowCount", creditOutflowCount
                )
        ));
    }

    private static List<CashflowMovementRecord> allMovements(
            List<CashflowMovementRecord> manualReview,
            List<CashflowMovementRecord> projectable,
            List<CashflowMovementRecord> rejected
    ) {
        var movements = new ArrayList<CashflowMovementRecord>(manualReview.size() + projectable.size() + rejected.size());
        movements.addAll(manualReview);
        movements.addAll(projectable);
        movements.addAll(rejected);
        return movements;
    }

    private static Optional<String> topRejectionReasonCode(List<CashflowMovementRecord> rejected) {
        return rejected.stream()
                .map(CashflowMovementRecord::rejectionReasonCode)
                .filter(reasonCode -> reasonCode != null && !reasonCode.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), HashMap::new, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        return numerator.multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private static int severityRank(HistorySignalResponse signal) {
        return "WARNING".equals(signal.severity()) ? 0 : 1;
    }

    private record CategoryConcentration(String categoryKey, BigDecimal amount, BigDecimal percent) {
    }

    public record HistoryRecommendationResponse(
            String profileId,
            Instant generatedAt,
            List<HistorySignalResponse> signals
    ) {
        public HistoryRecommendationResponse {
            if (profileId == null || profileId.isBlank()) {
                throw new IllegalArgumentException("Profile id is required");
            }
            if (generatedAt == null) {
                throw new IllegalArgumentException("Generated timestamp is required");
            }
            signals = List.copyOf(signals == null ? List.of() : signals);
        }
    }

    public record HistorySignalResponse(
            String type,
            String severity,
            String title,
            String description,
            String actionHint,
            Map<String, Object> metrics
    ) {
        public HistorySignalResponse {
            if (type == null || type.isBlank()) {
                throw new IllegalArgumentException("Signal type is required");
            }
            if (severity == null || severity.isBlank()) {
                throw new IllegalArgumentException("Severity is required");
            }
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Title is required");
            }
            if (description == null || description.isBlank()) {
                throw new IllegalArgumentException("Description is required");
            }
            if (actionHint == null || actionHint.isBlank()) {
                throw new IllegalArgumentException("Action hint is required");
            }
            metrics = Map.copyOf(metrics == null ? Map.of() : metrics);
        }
    }
}
