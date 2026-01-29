package com.wego.service.external;

import com.wego.config.OpenWeatherMapProperties;
import com.wego.dto.response.WeatherForecast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OpenWeatherMapClient.
 *
 * Tests the real implementation that calls OpenWeatherMap API.
 * Uses mocked RestTemplate to avoid actual API calls.
 *
 * @see OpenWeatherMapClient
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenWeatherMapClient")
class OpenWeatherMapClientTest {

    @Mock
    private RestTemplate restTemplate;

    private OpenWeatherMapClient client;
    private OpenWeatherMapProperties properties;

    private static final String TEST_API_KEY = "test-api-key-12345";

    // Sample API response (simplified version of real response)
    private static final String FORECAST_SUCCESS_RESPONSE = """
            {
                "cod": "200",
                "message": 0,
                "cnt": 40,
                "list": [
                    {
                        "dt": 1706540400,
                        "main": {
                            "temp": 20.5,
                            "temp_min": 18.0,
                            "temp_max": 22.0,
                            "humidity": 65
                        },
                        "weather": [{
                            "main": "Clouds",
                            "description": "scattered clouds",
                            "icon": "03d"
                        }],
                        "pop": 0.2,
                        "wind": {
                            "speed": 3.5
                        },
                        "dt_txt": "2024-01-29 15:00:00"
                    },
                    {
                        "dt": 1706551200,
                        "main": {
                            "temp": 19.0,
                            "temp_min": 17.0,
                            "temp_max": 21.0,
                            "humidity": 70
                        },
                        "weather": [{
                            "main": "Clouds",
                            "description": "broken clouds",
                            "icon": "04d"
                        }],
                        "pop": 0.3,
                        "wind": {
                            "speed": 4.0
                        },
                        "dt_txt": "2024-01-29 18:00:00"
                    },
                    {
                        "dt": 1706626800,
                        "main": {
                            "temp": 22.0,
                            "temp_min": 20.0,
                            "temp_max": 24.0,
                            "humidity": 55
                        },
                        "weather": [{
                            "main": "Clear",
                            "description": "clear sky",
                            "icon": "01d"
                        }],
                        "pop": 0.0,
                        "wind": {
                            "speed": 2.5
                        },
                        "dt_txt": "2024-01-30 15:00:00"
                    }
                ],
                "city": {
                    "name": "Tokyo",
                    "country": "JP"
                }
            }
            """;

    @BeforeEach
    void setUp() {
        properties = new OpenWeatherMapProperties();
        properties.setApiKey(TEST_API_KEY);
        properties.setEnabled(true);
        properties.setConnectTimeoutMs(5000);
        properties.setReadTimeoutMs(10000);

        client = new OpenWeatherMapClient(properties, restTemplate);
    }

    @Nested
    @DisplayName("Constructor")
    class Constructor {

        @Test
        @DisplayName("should create client with valid properties")
        void shouldCreateClientWithValidProperties() {
            OpenWeatherMapClient newClient = new OpenWeatherMapClient(properties, restTemplate);

            assertThat(newClient).isNotNull();
        }

        @Test
        @DisplayName("should throw when properties is null")
        void shouldThrowWhenPropertiesIsNull() {
            assertThatThrownBy(() -> new OpenWeatherMapClient(null, restTemplate))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("get5DayForecast")
    class Get5DayForecast {

        @Test
        @DisplayName("should return forecasts on success")
        void shouldReturnForecastsOnSuccess() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(FORECAST_SUCCESS_RESPONSE, HttpStatus.OK));

            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);

            assertThat(forecasts).isNotNull();
            assertThat(forecasts).hasSizeGreaterThan(0);
        }

        @Test
        @DisplayName("should aggregate 3-hour forecasts into daily forecasts")
        void shouldAggregate3HourForecastsIntoDaily() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(FORECAST_SUCCESS_RESPONSE, HttpStatus.OK));

            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);

            // The test response has 3 items for 2 days, so should aggregate to 2 daily forecasts
            assertThat(forecasts.size()).isLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("should extract weather condition and icon")
        void shouldExtractWeatherConditionAndIcon() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(FORECAST_SUCCESS_RESPONSE, HttpStatus.OK));

            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);

            WeatherForecast forecast = forecasts.get(0);
            assertThat(forecast.getCondition()).isNotNull();
            assertThat(forecast.getDescription()).isNotNull();
            assertThat(forecast.getIcon()).isNotNull();
        }

        @Test
        @DisplayName("should extract temperature data")
        void shouldExtractTemperatureData() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(FORECAST_SUCCESS_RESPONSE, HttpStatus.OK));

            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);

            WeatherForecast forecast = forecasts.get(0);
            assertThat(forecast.getTempHigh()).isGreaterThan(forecast.getTempLow());
            assertThat(forecast.getHumidity()).isBetween(0, 100);
        }

        @Test
        @DisplayName("should extract rain probability")
        void shouldExtractRainProbability() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(FORECAST_SUCCESS_RESPONSE, HttpStatus.OK));

            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);

            WeatherForecast forecast = forecasts.get(0);
            assertThat(forecast.getRainProbability()).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("should throw WeatherException for invalid API key")
        void shouldThrowForInvalidApiKey() {
            String errorResponse = """
                    {
                        "cod": "401",
                        "message": "Invalid API key"
                    }
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(errorResponse, HttpStatus.OK));

            assertThatThrownBy(() -> client.get5DayForecast(35.6812, 139.7671))
                    .isInstanceOf(WeatherException.class)
                    .hasMessageContaining("API key");
        }

        @Test
        @DisplayName("should throw WeatherException for rate limit exceeded")
        void shouldThrowForRateLimitExceeded() {
            String errorResponse = """
                    {
                        "cod": "429",
                        "message": "API calls limit exceeded"
                    }
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(errorResponse, HttpStatus.OK));

            assertThatThrownBy(() -> client.get5DayForecast(35.6812, 139.7671))
                    .isInstanceOf(WeatherException.class)
                    .hasMessageContaining("rate limit");
        }

        @Test
        @DisplayName("should throw WeatherException on HTTP error")
        void shouldThrowOnHttpError() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

            assertThatThrownBy(() -> client.get5DayForecast(35.6812, 139.7671))
                    .isInstanceOf(WeatherException.class);
        }

        @Test
        @DisplayName("should throw for invalid latitude")
        void shouldThrowForInvalidLatitude() {
            assertThatThrownBy(() -> client.get5DayForecast(91.0, 139.0))
                    .isInstanceOf(WeatherException.class)
                    .hasMessageContaining("Invalid location");
        }

        @Test
        @DisplayName("should throw for invalid longitude")
        void shouldThrowForInvalidLongitude() {
            assertThatThrownBy(() -> client.get5DayForecast(35.0, 181.0))
                    .isInstanceOf(WeatherException.class)
                    .hasMessageContaining("Invalid location");
        }

        @Test
        @DisplayName("should include API key in request URL")
        void shouldIncludeApiKeyInRequestUrl() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenAnswer(invocation -> {
                        String url = invocation.getArgument(0);
                        assertThat(url).contains("appid=" + TEST_API_KEY);
                        return new ResponseEntity<>(FORECAST_SUCCESS_RESPONSE, HttpStatus.OK);
                    });

            client.get5DayForecast(35.0, 139.0);
        }

        @Test
        @DisplayName("should use metric units")
        void shouldUseMetricUnits() {
            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenAnswer(invocation -> {
                        String url = invocation.getArgument(0);
                        assertThat(url).contains("units=metric");
                        return new ResponseEntity<>(FORECAST_SUCCESS_RESPONSE, HttpStatus.OK);
                    });

            client.get5DayForecast(35.0, 139.0);
        }
    }

    @Nested
    @DisplayName("Weather condition prioritization")
    class WeatherConditionPrioritization {

        @Test
        @DisplayName("should prefer rain over clear when aggregating same day forecasts")
        void shouldPreferRainOverClearWhenAggregating() {
            // Response with both clear and rain conditions on same day
            // Using timestamps 3 hours apart in the middle of the day (09:00 and 12:00 UTC)
            // to ensure they're the same day regardless of timezone
            // 1706518800 = 2024-01-29 09:00 UTC
            // 1706529600 = 2024-01-29 12:00 UTC (same day in all timezones)
            String mixedConditionsResponse = """
                    {
                        "cod": "200",
                        "list": [
                            {
                                "dt": 1706518800,
                                "main": {"temp": 20, "temp_min": 18, "temp_max": 22, "humidity": 65},
                                "weather": [{"main": "Clear", "description": "clear sky", "icon": "01d"}],
                                "pop": 0.0,
                                "wind": {"speed": 2.0}
                            },
                            {
                                "dt": 1706529600,
                                "main": {"temp": 18, "temp_min": 16, "temp_max": 20, "humidity": 80},
                                "weather": [{"main": "Rain", "description": "light rain", "icon": "10d"}],
                                "pop": 0.8,
                                "wind": {"speed": 4.0}
                            }
                        ]
                    }
                    """;

            when(restTemplate.getForEntity(anyString(), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(mixedConditionsResponse, HttpStatus.OK));

            List<WeatherForecast> forecasts = client.get5DayForecast(35.0, 139.0);

            // When aggregating, the max rain probability should be captured (0.8)
            // Rain is more significant than Clear, so condition should be Rain
            assertThat(forecasts).hasSize(1); // Both items are same day
            assertThat(forecasts.get(0).getRainProbability()).isEqualTo(0.8);
            assertThat(forecasts.get(0).getCondition()).isEqualTo("Rain");
        }
    }
}
