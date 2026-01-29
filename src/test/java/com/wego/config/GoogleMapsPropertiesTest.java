package com.wego.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GoogleMapsProperties configuration class.
 *
 * @see GoogleMapsProperties
 */
@DisplayName("GoogleMapsProperties")
class GoogleMapsPropertiesTest {

    @Nested
    @DisplayName("Default Values")
    class DefaultValues {

        @Test
        @DisplayName("should have null apiKey by default")
        void shouldHaveNullApiKeyByDefault() {
            GoogleMapsProperties properties = new GoogleMapsProperties();
            assertThat(properties.getApiKey()).isNull();
        }

        @Test
        @DisplayName("should be disabled by default")
        void shouldBeDisabledByDefault() {
            GoogleMapsProperties properties = new GoogleMapsProperties();
            assertThat(properties.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should have default connect timeout of 5000ms")
        void shouldHaveDefaultConnectTimeout() {
            GoogleMapsProperties properties = new GoogleMapsProperties();
            assertThat(properties.getConnectTimeoutMs()).isEqualTo(5000);
        }

        @Test
        @DisplayName("should have default read timeout of 10000ms")
        void shouldHaveDefaultReadTimeout() {
            GoogleMapsProperties properties = new GoogleMapsProperties();
            assertThat(properties.getReadTimeoutMs()).isEqualTo(10000);
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("should set apiKey correctly")
        void shouldSetApiKey() {
            GoogleMapsProperties properties = new GoogleMapsProperties();
            properties.setApiKey("test-api-key");
            assertThat(properties.getApiKey()).isEqualTo("test-api-key");
        }

        @Test
        @DisplayName("should set enabled correctly")
        void shouldSetEnabled() {
            GoogleMapsProperties properties = new GoogleMapsProperties();
            properties.setEnabled(true);
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should set connectTimeoutMs correctly")
        void shouldSetConnectTimeout() {
            GoogleMapsProperties properties = new GoogleMapsProperties();
            properties.setConnectTimeoutMs(3000);
            assertThat(properties.getConnectTimeoutMs()).isEqualTo(3000);
        }

        @Test
        @DisplayName("should set readTimeoutMs correctly")
        void shouldSetReadTimeout() {
            GoogleMapsProperties properties = new GoogleMapsProperties();
            properties.setReadTimeoutMs(15000);
            assertThat(properties.getReadTimeoutMs()).isEqualTo(15000);
        }
    }

    @Nested
    @DisplayName("Spring Configuration Binding")
    @SpringBootTest(classes = GoogleMapsPropertiesTest.TestConfig.class)
    @TestPropertySource(properties = {
        "wego.external-api.google-maps.api-key=test-key-123",
        "wego.external-api.google-maps.enabled=true",
        "wego.external-api.google-maps.connect-timeout-ms=3000",
        "wego.external-api.google-maps.read-timeout-ms=8000"
    })
    class SpringConfigurationBinding {

        @Autowired
        private GoogleMapsProperties properties;

        @Test
        @DisplayName("should bind apiKey from properties")
        void shouldBindApiKey() {
            assertThat(properties.getApiKey()).isEqualTo("test-key-123");
        }

        @Test
        @DisplayName("should bind enabled from properties")
        void shouldBindEnabled() {
            assertThat(properties.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should bind connectTimeoutMs from properties")
        void shouldBindConnectTimeout() {
            assertThat(properties.getConnectTimeoutMs()).isEqualTo(3000);
        }

        @Test
        @DisplayName("should bind readTimeoutMs from properties")
        void shouldBindReadTimeout() {
            assertThat(properties.getReadTimeoutMs()).isEqualTo(8000);
        }
    }

    @EnableConfigurationProperties(GoogleMapsProperties.class)
    static class TestConfig {
    }
}
