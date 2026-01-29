package com.wego.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for OpenWeatherMapProperties.
 *
 * Tests configuration property defaults and setters.
 *
 * @see OpenWeatherMapProperties
 */
@DisplayName("OpenWeatherMapProperties")
class OpenWeatherMapPropertiesTest {

    @Test
    @DisplayName("should have default values")
    void shouldHaveDefaultValues() {
        OpenWeatherMapProperties properties = new OpenWeatherMapProperties();

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getConnectTimeoutMs()).isEqualTo(5000);
        assertThat(properties.getReadTimeoutMs()).isEqualTo(10000);
        assertThat(properties.getApiKey()).isNull();
    }

    @Test
    @DisplayName("should allow setting api key")
    void shouldAllowSettingApiKey() {
        OpenWeatherMapProperties properties = new OpenWeatherMapProperties();
        properties.setApiKey("test-api-key");

        assertThat(properties.getApiKey()).isEqualTo("test-api-key");
    }

    @Test
    @DisplayName("should allow enabling the service")
    void shouldAllowEnablingTheService() {
        OpenWeatherMapProperties properties = new OpenWeatherMapProperties();
        properties.setEnabled(true);

        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should allow setting custom timeouts")
    void shouldAllowSettingCustomTimeouts() {
        OpenWeatherMapProperties properties = new OpenWeatherMapProperties();
        properties.setConnectTimeoutMs(3000);
        properties.setReadTimeoutMs(15000);

        assertThat(properties.getConnectTimeoutMs()).isEqualTo(3000);
        assertThat(properties.getReadTimeoutMs()).isEqualTo(15000);
    }
}
