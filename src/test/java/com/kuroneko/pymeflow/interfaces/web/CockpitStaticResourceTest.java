package com.kuroneko.pymeflow.interfaces.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CockpitStaticResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void servesRootCockpitWithAccessibleChileanCashflowIdentity() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<main")))
                .andExpect(content().string(containsString("PymeFlow")))
                .andExpect(content().string(containsString("caja diaria")))
                .andExpect(content().string(containsString("abonos")))
                .andExpect(content().string(containsString("cargos")))
                .andExpect(content().string(containsString("Cartola de movimientos")))
                .andExpect(content().string(containsString("fixture/demo")))
                .andExpect(content().string(containsString("No representa conectividad bancaria real")))
                .andExpect(content().string(not(containsString("mostrador"))))
                .andExpect(content().string(not(containsString("conectividad bancaria real habilitada"))));
    }

    @Test
    void servesRootCockpitWithPrimaryRegionsAndControls() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("aria-label=\"Resumen de caja diaria\"")))
                .andExpect(content().string(containsString("Riel de comprobantes")))
                .andExpect(content().string(containsString("aria-label=\"Acciones principales del cockpit\"")))
                .andExpect(content().string(containsString("<button type=\"button\"")))
                .andExpect(content().string(containsString("Revisar abonos y cargos")));
    }

    @Test
    void servesCockpitScriptWithSameOriginApiWiringAndSafeStateTargets() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-api-target=\"cash-summary\"")))
                .andExpect(content().string(containsString("data-api-target=\"sync-receipt\"")))
                .andExpect(content().string(containsString("data-api-target=\"ledger-list\"")))
                .andExpect(content().string(containsString("data-api-target=\"review-list\"")));

        mockMvc.perform(get("/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/api/profiles/active")))
                .andExpect(content().string(containsString("/api/profiles/active/categories")))
                .andExpect(content().string(containsString("/api/cashflow/provider-syncs")))
                .andExpect(content().string(containsString("/api/cashflow/imports/manual")))
                .andExpect(content().string(containsString("/api/cashflow/history/manual-review")))
                .andExpect(content().string(containsString("/api/cashflow/history/projection-ready")))
                .andExpect(content().string(containsString("/api/cashflow/recommendations")))
                .andExpect(content().string(containsString("Sin datos para mostrar")))
                .andExpect(content().string(containsString("No se pudo cargar")))
                .andExpect(content().string(not(containsString("credentialRef"))))
                .andExpect(content().string(not(containsString("cursor"))))
                .andExpect(content().string(not(containsString("token"))));
    }
}
