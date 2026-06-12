package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.vertical.VerticalProfileService;
import com.kuroneko.pymeflow.domain.vertical.CashflowCategory;
import com.kuroneko.pymeflow.domain.vertical.CashflowDirection;
import com.kuroneko.pymeflow.domain.vertical.ProfileId;
import com.kuroneko.pymeflow.domain.vertical.ProfileRule;
import com.kuroneko.pymeflow.domain.vertical.VerticalProfile;
import com.kuroneko.pymeflow.infrastructure.config.VerticalProfileProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileController.class)
class ProfileControllerTest {

    @MockBean
    private VerticalProfileService verticalProfileService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsActiveProfileRulesAndCategories() throws Exception {
        when(verticalProfileService.loadProfile(new ProfileId("pharmacy-cl"))).thenReturn(profile());

        mockMvc.perform(get("/api/profiles/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("pharmacy-cl"));

        mockMvc.perform(get("/api/profiles/active/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ruleKey").value("low-balance-warning"));

        mockMvc.perform(get("/api/profiles/active/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].key").value("sales"));
    }

    private static VerticalProfile profile() {
        return new VerticalProfile(
                new ProfileId("pharmacy-cl"),
                "Farmacia chilena",
                List.of(new ProfileRule("low-balance-warning", "projected_balance_below_threshold", BigDecimal.valueOf(250000), "low-balance-warning")),
                List.of(new CashflowCategory("sales", "Ventas", CashflowDirection.INFLOW)),
                List.of()
        );
    }

    @TestConfiguration
    static class ControllerTestConfiguration {
        @Bean
        VerticalProfileProperties verticalProfileProperties() {
            return new VerticalProfileProperties(
                    "pharmacy-cl",
                    new VerticalProfileProperties.MockProviders(false, false),
                    "classpath:templates/pharmacy-recommendations.es.mustache",
                    List.of("receta"),
                    List.of(new VerticalProfileProperties.ProfileDefinition(
                            "pharmacy-cl",
                            "Farmacia chilena",
                            List.of(new VerticalProfileProperties.CategoryDefinition("sales", "Ventas", "INFLOW")),
                            List.of(),
                            List.of(),
                            Map.of()
                    ))
            );
        }
    }
}
