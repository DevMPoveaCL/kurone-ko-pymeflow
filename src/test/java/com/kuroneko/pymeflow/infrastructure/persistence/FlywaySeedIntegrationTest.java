package com.kuroneko.pymeflow.infrastructure.persistence;

import com.kuroneko.pymeflow.application.cashflow.CashflowIngestionService;
import com.kuroneko.pymeflow.application.cashflow.SensitiveDataPolicy;
import com.kuroneko.pymeflow.application.port.out.CashflowCategorizationPort;
import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.cashflow.CategoryAssignment;
import com.kuroneko.pymeflow.domain.cashflow.Transaction;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FlywaySeedIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seedsVerticalProfileCategories() {
        Integer categories = jdbcTemplate.queryForObject(
                "select count(*) from vertical_profile_categories where profile_id = 'pharmacy-cl'",
                Integer.class);

        assertThat(categories).isEqualTo(9);
    }

    @Test
    void loadsProfileFromSeededDatabaseAndRunsCategorizationPipeline() {
        var adapter = new VerticalProfileJpaAdapter(jdbcTemplate);
        var profileService = new VerticalProfileService(adapter);
        CashflowCategorizationPort categorizationPort = (transaction, profile) -> profile.categories().stream()
                .filter(category -> category.key().equals("sales"))
                .findFirst()
                .map(category -> new CategoryAssignment(Optional.of(category), false))
                .orElseGet(() -> new CategoryAssignment(Optional.empty(), true));
        var service = new CashflowIngestionService(
                profileService,
                categorizationPort,
                new SensitiveDataPolicy(List.of()),
                new CashflowMovementHistoryJdbcAdapter(jdbcTemplate)
        );

        var result = service.ingest(new CashflowIngestionService.CashflowIngestionCommand(
                new ProfileId("pharmacy-cl"),
                List.of(new Transaction("Venta Caja 1", BigDecimal.valueOf(1000), Currency.getInstance("CLP"), LocalDate.of(2026, 6, 1)))
        ));

        assertThat(result.rejected()).isEmpty();
        assertThat(result.categorized()).singleElement()
                .extracting(item -> item.assignment().category().orElseThrow().key())
                .isEqualTo("sales");
    }

    @Test
    void verifiesSeededRulesAndObligationTemplates() {
        Integer rules = jdbcTemplate.queryForObject(
                "select count(*) from vertical_profile_rules where profile_id = 'pharmacy-cl'",
                Integer.class);
        Integer obligations = jdbcTemplate.queryForObject(
                "select count(*) from vertical_profile_obligation_templates where profile_id = 'pharmacy-cl'",
                Integer.class);

        assertThat(rules).isEqualTo(3);
        assertThat(obligations).isEqualTo(4);
    }
}
