package com.kuroneko.pymeflow.infrastructure.config;

import com.kuroneko.pymeflow.application.port.out.ProfileRegistryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class VerticalProfilePropertiesTest {

    @MockBean
    private ProfileRegistryPort profileRegistryPort;

    @Autowired
    private VerticalProfileProperties properties;

    @Test
    void loadsConfiguredVerticalProfileDefaults() {
        assertThat(properties.activeProfileId()).isEqualTo("pharmacy-cl");
        assertThat(properties.profiles()).hasSize(1);
        assertThat(properties.profiles().getFirst().categories()).hasSize(9);
        assertThat(properties.recommendationTemplatePath()).contains("recommendations.es.mustache");
    }
}
