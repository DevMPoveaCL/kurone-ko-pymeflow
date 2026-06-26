package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cockpit.CockpitPreferencesService;
import com.kuroneko.pymeflow.domain.cockpit.CockpitPreferences;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CockpitPreferencesController.class)
class CockpitPreferencesControllerTest {

    @MockBean
    private CockpitPreferencesService service;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsSafeDefaultsWhenProfileHasNoPreferences() throws Exception {
        when(service.load(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cashflow/cockpit/preferences")
                        .param("profileId", "pharmacy-cl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").doesNotExist())
                .andExpect(jsonPath("$.preferredHorizonDays").value(7))
                .andExpect(jsonPath("$.balanceSource").value("USER_ENTERED_MANUAL"))
                .andExpect(content().string(not(containsString("bank"))))
                .andExpect(content().string(not(containsString("live"))));
    }

    @Test
    void returnsPersistedPreferencesWithoutBankLiveSemantics() throws Exception {
        when(service.load(any())).thenReturn(Optional.of(new CockpitPreferences(BigDecimal.valueOf(1_500_000), 30)));

        mockMvc.perform(get("/api/cashflow/cockpit/preferences")
                        .param("profileId", "pharmacy-cl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value(1500000))
                .andExpect(jsonPath("$.preferredHorizonDays").value(30))
                .andExpect(jsonPath("$.balanceSource").value("USER_ENTERED_MANUAL"))
                .andExpect(content().string(not(containsString("bank"))))
                .andExpect(content().string(not(containsString("live"))));
    }

    @Test
    void rejectsInvalidHorizonDaysWithSafeValidationError() throws Exception {
        mockMvc.perform(put("/api/cashflow/cockpit/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "openingBalance": 1500000,
                                  "preferredHorizonDays": 15
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(content().string(containsString("7 o 30")));
    }

    @Test
    void rejectsUnsafeOpeningBalanceWithSafeValidationError() throws Exception {
        mockMvc.perform(put("/api/cashflow/cockpit/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "openingBalance": 1000000000000000000,
                                  "preferredHorizonDays": 7
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(content().string(containsString("El saldo inicial debe ser un valor seguro.")));
    }

    @Test
    void rejectsNonNumericOpeningBalanceWithSafeValidationError() throws Exception {
        mockMvc.perform(put("/api/cashflow/cockpit/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "openingBalance": "not-a-number",
                                  "preferredHorizonDays": 7
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(content().string(containsString("saldo inicial")));
    }

    @Test
    void savesValidPreferencesAndReturnsApiShape() throws Exception {
        when(service.save(any(), any(), org.mockito.ArgumentMatchers.eq(7)))
                .thenReturn(new CockpitPreferences(BigDecimal.valueOf(350_000), 7));

        mockMvc.perform(put("/api/cashflow/cockpit/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileId": "pharmacy-cl",
                                  "openingBalance": 350000,
                                  "preferredHorizonDays": 7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openingBalance").value(350000))
                .andExpect(jsonPath("$.preferredHorizonDays").value(7))
                .andExpect(jsonPath("$.balanceSource").value("USER_ENTERED_MANUAL"));
    }
}
