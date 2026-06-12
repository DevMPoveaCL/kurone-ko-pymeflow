package com.kuroneko.pymeflow.infrastructure.persistence;

import com.kuroneko.pymeflow.application.cashflow.CashflowMovementDraft;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementRecord;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementStatus;
import com.kuroneko.pymeflow.application.cashflow.ManualReviewMovementResolutionCommand;
import com.kuroneko.pymeflow.application.port.out.CashflowMovementHistoryPort;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnBean(JdbcTemplate.class)
public class CashflowMovementHistoryJdbcAdapter implements CashflowMovementHistoryPort {
    private static final String SELECT_COLUMNS = """
            select id, profile_id, amount, currency, movement_date, status, category_key,
                   safe_description, source_reference, rejection_reason_code,
                   resolved_at, created_at, updated_at
            from cashflow_movement_history
            """;

    private final JdbcTemplate jdbcTemplate;

    public CashflowMovementHistoryJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CashflowMovementRecord> saveAll(List<CashflowMovementDraft> drafts) {
        return List.copyOf(drafts == null ? List.of() : drafts).stream()
                .map(this::save)
                .toList();
    }

    @Override
    public Optional<CashflowMovementRecord> findById(UUID movementId) {
        var rows = jdbcTemplate.query(
                SELECT_COLUMNS + " where id = ?",
                (rs, rowNum) -> mapRow(rs),
                movementId
        );
        return rows.stream().findFirst();
    }

    @Override
    public List<CashflowMovementRecord> findPendingManualReviews(ProfileId profileId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " where profile_id = ? and status = ? order by movement_date, created_at",
                (rs, rowNum) -> mapRow(rs),
                profileId.value(),
                CashflowMovementStatus.MANUAL_REVIEW.name()
        );
    }

    @Override
    public List<CashflowMovementRecord> findProjectionReady(ProfileId profileId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " where profile_id = ? and status = ? order by movement_date, created_at",
                (rs, rowNum) -> mapRow(rs),
                profileId.value(),
                CashflowMovementStatus.PROJECTABLE.name()
        );
    }

    @Override
    public Optional<CashflowMovementRecord> resolveManualReview(ManualReviewMovementResolutionCommand command) {
        var updated = jdbcTemplate.update("""
                        update cashflow_movement_history
                        set status = ?, category_key = ?, resolved_at = ?, updated_at = ?
                        where id = ? and profile_id = ? and status = ?
                        """,
                CashflowMovementStatus.PROJECTABLE.name(),
                command.categoryKey(),
                Timestamp.from(Instant.now()),
                Timestamp.from(Instant.now()),
                command.movementId(),
                command.profileId().value(),
                CashflowMovementStatus.MANUAL_REVIEW.name()
        );
        if (updated != 1) {
            return Optional.empty();
        }
        return findById(command.movementId());
    }

    private CashflowMovementRecord save(CashflowMovementDraft draft) {
        var id = UUID.randomUUID();
        var now = Instant.now();
        jdbcTemplate.update("""
                        insert into cashflow_movement_history
                        (id, profile_id, amount, currency, movement_date, status, category_key,
                         safe_description, source_reference, rejection_reason_code,
                         resolved_at, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                draft.profileId().value(),
                draft.amount(),
                draft.currency().getCurrencyCode(),
                draft.date(),
                draft.status().name(),
                draft.categoryKey(),
                draft.safeDescription(),
                draft.sourceReference(),
                draft.rejectionReasonCode(),
                null,
                Timestamp.from(now),
                Timestamp.from(now)
        );
        return findById(id).orElseThrow(() -> new IllegalStateException("Saved movement was not found"));
    }

    private CashflowMovementRecord mapRow(ResultSet rs) throws SQLException {
        return new CashflowMovementRecord(
                rs.getObject("id", UUID.class),
                new ProfileId(rs.getString("profile_id")),
                rs.getBigDecimal("amount"),
                Currency.getInstance(rs.getString("currency")),
                rs.getDate("movement_date").toLocalDate(),
                CashflowMovementStatus.valueOf(rs.getString("status")),
                rs.getString("category_key"),
                rs.getString("safe_description"),
                rs.getString("source_reference"),
                rs.getString("rejection_reason_code"),
                instantOrNull(rs, "resolved_at"),
                instantOrNull(rs, "created_at"),
                instantOrNull(rs, "updated_at")
        );
    }

    private static Instant instantOrNull(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
