package com.kuroneko.pymeflow.interfaces.web;

import com.kuroneko.pymeflow.application.cockpit.CockpitDemoResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DemoDataController.class)
class DemoDataControllerTest {
    private static final String ENDPOINT = "/api/cockpit/demo/reset-and-seed";

    @MockBean
    private CockpitDemoResetService service;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void resetAndSeedReturnsSafeDemoResponseForValidProfile() throws Exception {
        when(service.resetAndSeed(any())).thenReturn(new CockpitDemoResetService.DemoResetResult(
                "DEMO_RESET_SEEDED",
                5,
                "sync-demo-001",
                "Demo data was reset and seeded safely."
        ));

        mockMvc.perform(post(ENDPOINT).param("profileId", "pharmacy-cl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEMO_RESET_SEEDED"))
                .andExpect(jsonPath("$.movementsSeeded").value(5))
                .andExpect(jsonPath("$.syncSessionId").value("sync-demo-001"))
                .andExpect(jsonPath("$.message").value("Demo data was reset and seeded safely."))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("token"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }

    @Test
    void rejectsMissingProfileIdWithoutInvokingService() throws Exception {
        mockMvc.perform(post(ENDPOINT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(content().string(containsString("El perfil es obligatorio.")));

        verify(service, never()).resetAndSeed(any());
    }

    @Test
    void rejectsBlankProfileIdWithoutInvokingService() throws Exception {
        mockMvc.perform(post(ENDPOINT).param("profileId", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(service, never()).resetAndSeed(any());
    }

    @Test
    void rejectsNonDemoProfileWithSafeDemoOnlyMessage() throws Exception {
        when(service.resetAndSeed(any()))
                .thenThrow(new CockpitDemoResetService.DemoOnlyProfileException("Demo reset is available for demo profiles only."));

        mockMvc.perform(post(ENDPOINT).param("profileId", "retail-cl"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("DEMO_ONLY_PROFILE_REQUIRED"))
                .andExpect(jsonPath("$.message").value("Esta acción está disponible solo para datos de demostración."))
                .andExpect(content().string(not(containsString("retail-cl"))));
    }

    @Test
    void serviceFailureReturnsSafeGenericErrorWithoutInternalDetails() throws Exception {
        when(service.resetAndSeed(any())).thenThrow(new IllegalStateException("database password leaked stackTrace"));

        mockMvc.perform(post(ENDPOINT).param("profileId", "pharmacy-cl"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("DEMO_RESET_FAILED"))
                .andExpect(jsonPath("$.message").value("No fue posible reiniciar los datos de demostración."))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("stackTrace"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }
}
