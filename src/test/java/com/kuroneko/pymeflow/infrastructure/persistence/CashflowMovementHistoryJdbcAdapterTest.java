package com.kuroneko.pymeflow.infrastructure.persistence;

import com.kuroneko.pymeflow.application.cashflow.CashflowMovementDraft;
import com.kuroneko.pymeflow.application.cashflow.CashflowMovementStatus;
import com.kuroneko.pymeflow.application.cashflow.ManualReviewMovementResolutionCommand;
import com.kuroneko.pymeflow.domain.cashflow.TransactionDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest(properties = {
        "spring.flyway.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:cashflow-history;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CashflowMovementHistoryJdbcAdapterTest {

    private static final ProfileId PROFILE_ID = new ProfileId("test-retail-cl");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private CashflowMovementHistoryJdbcAdapter adapter;

    @BeforeEach
    void setUp() throws Exception {
        adapter = new CashflowMovementHistoryJdbcAdapter(jdbcTemplate);
        jdbcTemplate.execute("create table if not exists vertical_profiles (id varchar(63) primary key, display_name varchar(120) not null, enabled boolean not null, created_at timestamp with time zone not null default now())");
        jdbcTemplate.execute("create table if not exists vertical_profile_categories (profile_id varchar(63) not null references vertical_profiles(id), category_key varchar(80) not null, display_name varchar(120) not null, direction varchar(20) not null, sort_order integer not null, primary key (profile_id, category_key))");
        jdbcTemplate.execute("create table if not exists vertical_profile_rules (profile_id varchar(63) not null references vertical_profiles(id), rule_key varchar(100) not null, condition_key varchar(120) not null, threshold numeric(18, 2) not null, action_key varchar(100) not null, primary key (profile_id, rule_key))");
        jdbcTemplate.execute("create table if not exists vertical_profile_obligation_templates (profile_id varchar(63) not null references vertical_profiles(id), obligation_key varchar(100) not null, display_name varchar(120) not null, estimated_amount numeric(18, 2) not null, frequency varchar(20) not null, due_day_of_month integer not null, primary key (profile_id, obligation_key))");
        jdbcTemplate.execute("drop table if exists cashflow_movement_history");
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V2__create_cashflow_movement_history.sql"));
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/migration/V4__add_movement_direction.sql"));
        }
        createSourceReferenceUniqueIndexForH2();
        jdbcTemplate.update("delete from cashflow_movement_history");
        jdbcTemplate.update("delete from vertical_profile_categories");
        jdbcTemplate.update("delete from vertical_profile_rules");
        jdbcTemplate.update("delete from vertical_profile_obligation_templates");
        jdbcTemplate.update("delete from vertical_profiles");
        jdbcTemplate.update("insert into vertical_profiles (id, display_name, enabled) values (?, ?, true)", PROFILE_ID.value(), "Comercio prueba");
        jdbcTemplate.update("insert into vertical_profile_categories (profile_id, category_key, display_name, direction, sort_order) values (?, ?, ?, ?, ?)", PROFILE_ID.value(), "sales", "Ventas", "INFLOW", 10);
        jdbcTemplate.update("insert into vertical_profile_categories (profile_id, category_key, display_name, direction, sort_order) values (?, ?, ?, ?, ?)", PROFILE_ID.value(), "supplies", "Insumos", "OUTFLOW", 20);
    }

    private void createSourceReferenceUniqueIndexForH2() {
        jdbcTemplate.execute("""
                create unique index if not exists idx_cashflow_movement_history_profile_source
                on cashflow_movement_history(profile_id, source_reference)
                """);
    }

    @Test
    void savesAndReadsMovementByIdWithNullableSafeFields() {
        var saved = adapter.saveAll(List.of(manualReview(null, null))).getFirst();

        var found = adapter.findById(saved.id());

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().safeDescription()).isNull();
        assertThat(found.orElseThrow().sourceReference()).isNull();
        assertThat(found.orElseThrow().status()).isEqualTo(CashflowMovementStatus.MANUAL_REVIEW);
    }

    @Test
    void listsOnlyPendingManualReviewsForProfile() {
        var otherProfile = new ProfileId("other-retail-cl");
        jdbcTemplate.update("insert into vertical_profiles (id, display_name, enabled) values (?, ?, true)", otherProfile.value(), "Otro comercio");
        adapter.saveAll(List.of(
                manualReview("Venta Caja 1", "batch-001"),
                projectable("sales"),
                new CashflowMovementDraft(otherProfile, BigDecimal.valueOf(1500), Currency.getInstance("CLP"), LocalDate.of(2026, 6, 3), CashflowMovementStatus.MANUAL_REVIEW, null, "Venta Caja 2", null, null)
        ));

        var pending = adapter.findPendingManualReviews(PROFILE_ID);

        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().safeDescription()).isEqualTo("Venta Caja 1");
        assertThat(pending.getFirst().status()).isEqualTo(CashflowMovementStatus.MANUAL_REVIEW);
    }

    @Test
    void listsOnlyProjectionReadyMovementsForProfile() {
        adapter.saveAll(List.of(
                manualReview("Venta Caja 1", null),
                projectable("sales"),
                rejected("policy-blocked")
        ));

        var projectionReady = adapter.findProjectionReady(PROFILE_ID);

        assertThat(projectionReady).hasSize(1);
        assertThat(projectionReady.getFirst().categoryKey()).isEqualTo("sales");
        assertThat(projectionReady.getFirst().status()).isEqualTo(CashflowMovementStatus.PROJECTABLE);
    }

    @Test
    void findsMovementsByStatusForProfileOrderedByMovementDate() {
        var otherProfile = new ProfileId("other-retail-cl");
        jdbcTemplate.update("insert into vertical_profiles (id, display_name, enabled) values (?, ?, true)", otherProfile.value(), "Otro comercio");
        jdbcTemplate.update("insert into vertical_profile_categories (profile_id, category_key, display_name, direction, sort_order) values (?, ?, ?, ?, ?)", otherProfile.value(), "sales", "Ventas", "INFLOW", 10);
        adapter.saveAll(List.of(
                projectable("supplies", LocalDate.of(2026, 6, 4), "batch-004"),
                manualReview("Venta Caja 1", "batch-001"),
                projectable("sales", LocalDate.of(2026, 6, 2), "batch-002"),
                rejected("policy-blocked"),
                new CashflowMovementDraft(otherProfile, BigDecimal.valueOf(1800), Currency.getInstance("CLP"), LocalDate.of(2026, 6, 1), CashflowMovementStatus.PROJECTABLE, "sales", "Venta otro perfil", "other-001", null)
        ));

        var projectable = adapter.findByStatus(PROFILE_ID, CashflowMovementStatus.PROJECTABLE);
        var manualReview = adapter.findByStatus(PROFILE_ID, CashflowMovementStatus.MANUAL_REVIEW);
        var rejected = adapter.findByStatus(PROFILE_ID, CashflowMovementStatus.REJECTED);

        assertThat(projectable)
                .extracting(record -> record.categoryKey())
                .containsExactly("sales", "supplies");
        assertThat(projectable)
                .extracting(record -> record.profileId())
                .containsOnly(PROFILE_ID);
        assertThat(manualReview)
                .hasSize(1)
                .first()
                .satisfies(record -> {
                    assertThat(record.status()).isEqualTo(CashflowMovementStatus.MANUAL_REVIEW);
                    assertThat(record.safeDescription()).isEqualTo("Venta Caja 1");
                });
        assertThat(rejected)
                .hasSize(1)
                .first()
                .satisfies(record -> {
                    assertThat(record.status()).isEqualTo(CashflowMovementStatus.REJECTED);
                    assertThat(record.rejectionReasonCode()).isEqualTo("policy-blocked");
                    assertThat(record.safeDescription()).isNull();
                });
    }

    @Test
    void findByStatusReturnsEmptyWhenNoMovementMatchesStatus() {
        adapter.saveAll(List.of(manualReview("Venta Caja 1", "batch-001")));

        var rejected = adapter.findByStatus(PROFILE_ID, CashflowMovementStatus.REJECTED);

        assertThat(rejected).isEmpty();
    }

    @Test
    void resolvesPendingManualReviewWithAtomicStatusTransition() {
        var movement = adapter.saveAll(List.of(manualReview("Venta Caja 1", null))).getFirst();

        var resolved = adapter.resolveManualReview(new ManualReviewMovementResolutionCommand(movement.id(), PROFILE_ID, "sales"));
        var repeated = adapter.resolveManualReview(new ManualReviewMovementResolutionCommand(movement.id(), PROFILE_ID, "supplies"));

        assertThat(resolved).isPresent();
        assertThat(resolved.orElseThrow().status()).isEqualTo(CashflowMovementStatus.PROJECTABLE);
        assertThat(resolved.orElseThrow().categoryKey()).isEqualTo("sales");
        assertThat(resolved.orElseThrow().resolvedAt()).isNotNull();
        assertThat(repeated).isEmpty();
        assertThat(adapter.findById(movement.id()).orElseThrow().categoryKey()).isEqualTo("sales");
    }

    @Test
    void rejectedMovementCannotBeResolvedAndResolvedMovementBecomesProjectionReady() {
        var rejected = adapter.saveAll(List.of(rejected("policy-blocked"))).getFirst();
        var manualReview = adapter.saveAll(List.of(manualReview("Venta Caja 1", "caja-1"))).getFirst();

        var rejectedResolution = adapter.resolveManualReview(new ManualReviewMovementResolutionCommand(rejected.id(), PROFILE_ID, "sales"));
        var resolved = adapter.resolveManualReview(new ManualReviewMovementResolutionCommand(manualReview.id(), PROFILE_ID, "sales"));

        assertThat(rejectedResolution).isEmpty();
        assertThat(adapter.findById(rejected.id()).orElseThrow().status()).isEqualTo(CashflowMovementStatus.REJECTED);
        assertThat(resolved).isPresent();
        assertThat(adapter.findProjectionReady(PROFILE_ID))
                .extracting(record -> record.id())
                .contains(manualReview.id())
                .doesNotContain(rejected.id());
    }

    @Test
    void findsMovementByProfileAndSourceReference() {
        var otherProfile = new ProfileId("other-retail-cl");
        jdbcTemplate.update("insert into vertical_profiles (id, display_name, enabled) values (?, ?, true)", otherProfile.value(), "Otro comercio");
        jdbcTemplate.update("insert into vertical_profile_categories (profile_id, category_key, display_name, direction, sort_order) values (?, ?, ?, ?, ?)", otherProfile.value(), "sales", "Ventas", "INFLOW", 10);
        var saved = adapter.saveAll(List.of(
                manualReview("Venta Caja 1", "batch-001"),
                new CashflowMovementDraft(otherProfile, BigDecimal.valueOf(1300), Currency.getInstance("CLP"), LocalDate.of(2026, 6, 1), CashflowMovementStatus.MANUAL_REVIEW, null, "Venta Caja 1", "batch-001", null)
        )).getFirst();

        var found = adapter.findBySourceReference(PROFILE_ID, "batch-001");

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().id()).isEqualTo(saved.id());
        assertThat(adapter.findBySourceReference(PROFILE_ID, "missing")).isEmpty();
    }

    @Test
    void duplicateProfileAndSourceReferenceReturnsExistingMovement() {
        var first = adapter.saveAll(List.of(manualReview("Venta Caja 1", "batch-001"))).getFirst();

        var second = adapter.saveAll(List.of(manualReview("Venta Caja 1 replay", "batch-001"))).getFirst();

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(adapter.findPendingManualReviews(PROFILE_ID)).hasSize(1);
        assertThat(second.safeDescription()).isEqualTo("Venta Caja 1");
    }

    @Test
    void duplicateFingerprintSourceReferenceReturnsExistingMovementThroughUniqueIndexCatchPath() {
        var sourceReference = "fp:v1:4480441486d2480c6bd52d41052f0814d6a50853787d3bd4540f71482fd6a056";
        var first = adapter.saveAll(List.of(manualReview("Venta Caja 1", sourceReference))).getFirst();

        var second = adapter.saveAll(List.of(manualReview("Venta Caja 1 retry", sourceReference))).getFirst();

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.sourceReference()).isEqualTo(sourceReference);
        assertThat(second.safeDescription()).isEqualTo("Venta Caja 1");
    }

    @Test
    void savesAndReadsDebitMovementDirection() {
        var saved = adapter.saveAll(List.of(new CashflowMovementDraft(
                PROFILE_ID,
                BigDecimal.valueOf(1200),
                Currency.getInstance("CLP"),
                LocalDate.of(2026, 6, 1),
                TransactionDirection.DEBIT,
                CashflowMovementStatus.MANUAL_REVIEW,
                null,
                "Proveedor insumos",
                "batch-debit-001",
                null
        ))).getFirst();

        var found = adapter.findById(saved.id());

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().direction()).isEqualTo(TransactionDirection.DEBIT);
    }

    @Test
    void readsLegacyRowsWithoutMovementDirectionAsCreditDefault() {
        var movementId = UUID.randomUUID();
        jdbcTemplate.update("""
                        insert into cashflow_movement_history
                        (id, profile_id, amount, currency, movement_date, status, category_key,
                         safe_description, source_reference, rejection_reason_code, resolved_at, created_at, updated_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now(), now())
                        """,
                movementId,
                PROFILE_ID.value(),
                BigDecimal.valueOf(2000),
                "CLP",
                LocalDate.of(2026, 6, 2),
                CashflowMovementStatus.PROJECTABLE.name(),
                "sales",
                "Venta histórica",
                "legacy-credit-001",
                null
        );

        var found = adapter.findById(movementId);

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().direction()).isEqualTo(TransactionDirection.CREDIT);
    }

    private static CashflowMovementDraft manualReview(String safeDescription, String sourceReference) {
        return new CashflowMovementDraft(
                PROFILE_ID,
                BigDecimal.valueOf(1200),
                Currency.getInstance("CLP"),
                LocalDate.of(2026, 6, 1),
                CashflowMovementStatus.MANUAL_REVIEW,
                null,
                safeDescription,
                sourceReference,
                null
        );
    }

    private static CashflowMovementDraft projectable(String categoryKey) {
        return projectable(categoryKey, LocalDate.of(2026, 6, 2), "batch-002");
    }

    private static CashflowMovementDraft projectable(String categoryKey, LocalDate movementDate, String sourceReference) {
        return new CashflowMovementDraft(
                PROFILE_ID,
                BigDecimal.valueOf(2500),
                Currency.getInstance("CLP"),
                movementDate,
                CashflowMovementStatus.PROJECTABLE,
                categoryKey,
                "Venta Caja 1",
                sourceReference,
                null
        );
    }

    private static CashflowMovementDraft rejected(String reasonCode) {
        return new CashflowMovementDraft(
                PROFILE_ID,
                BigDecimal.valueOf(900),
                Currency.getInstance("CLP"),
                LocalDate.of(2026, 6, 3),
                CashflowMovementStatus.REJECTED,
                null,
                null,
                null,
                reasonCode
        );
    }
}
