package com.wego.service.external;

import com.wego.dto.response.WeatherForecast;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MockWeatherClient.
 *
 * Tests the mock implementation for development and testing.
 *
 * @see MockWeatherClient
 */
@DisplayName("MockWeatherClient")
class MockWeatherClientTest {

    private MockWeatherClient client;

    @BeforeEach
    void setUp() {
        client = new MockWeatherClient();
    }

    @Nested
    @DisplayName("get5DayForecast")
    class Get5DayForecast {

        @Test
        @DisplayName("should return 5 days of forecasts")
        void shouldReturn5DaysOfForecasts() {
            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);

            assertThat(forecasts).hasSize(5);
        }

        @Test
        @DisplayName("should return forecasts starting from today")
        void shouldReturnForecastsStartingFromToday() {
            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);

            LocalDate today = LocalDate.now();
            assertThat(forecasts.get(0).getDate()).isEqualTo(today);
            assertThat(forecasts.get(1).getDate()).isEqualTo(today.plusDays(1));
            assertThat(forecasts.get(4).getDate()).isEqualTo(today.plusDays(4));
        }

        @Test
        @DisplayName("should return valid condition strings")
        void shouldReturnValidConditionStrings() {
            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);

            String[] validConditions = {"Clear", "Clouds", "Rain", "Drizzle", "Thunderstorm", "Snow"};

            for (WeatherForecast forecast : forecasts) {
                assertThat(forecast.getCondition())
                        .isIn((Object[]) validConditions);
            }
        }

        @Test
        @DisplayName("should return temperature in realistic range")
        void shouldReturnTemperatureInRealisticRange() {
            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);

            for (WeatherForecast forecast : forecasts) {
                assertThat(forecast.getTempHigh()).isBetween(-30.0, 50.0);
                assertThat(forecast.getTempLow()).isBetween(-40.0, 45.0);
                assertThat(forecast.getTempHigh()).isGreaterThanOrEqualTo(forecast.getTempLow());
            }
        }

        @Test
        @DisplayName("should return humidity between 0 and 100")
        void shouldReturnHumidityBetween0And100() {
            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);

            for (WeatherForecast forecast : forecasts) {
                assertThat(forecast.getHumidity()).isBetween(0, 100);
            }
        }

        @Test
        @DisplayName("should return rain probability between 0 and 1")
        void shouldReturnRainProbabilityBetween0And1() {
            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);

            for (WeatherForecast forecast : forecasts) {
                assertThat(forecast.getRainProbability()).isBetween(0.0, 1.0);
            }
        }

    }

    @Nested
    @DisplayName("Coordinate validation")
    class CoordinateValidation {

        @Test
        @DisplayName("should accept valid coordinates")
        void shouldAcceptValidCoordinates() {
            // Various valid coordinates should not throw
            assertThat(client.get5DayForecast(0, 0)).isNotNull();
            assertThat(client.get5DayForecast(-90, -180)).isNotNull();
            assertThat(client.get5DayForecast(90, 180)).isNotNull();
            assertThat(client.get5DayForecast(45.5, -73.5)).isNotNull(); // Montreal
        }

        @Test
        @DisplayName("should throw for invalid latitude > 90")
        void shouldThrowForLatitudeAbove90() {
            assertThatThrownBy(() -> client.get5DayForecast(91, 0))
                    .isInstanceOf(WeatherException.class);
        }

        @Test
        @DisplayName("should throw for invalid latitude < -90")
        void shouldThrowForLatitudeBelow90() {
            assertThatThrownBy(() -> client.get5DayForecast(-91, 0))
                    .isInstanceOf(WeatherException.class);
        }

        @Test
        @DisplayName("should throw for invalid longitude > 180")
        void shouldThrowForLongitudeAbove180() {
            assertThatThrownBy(() -> client.get5DayForecast(0, 181))
                    .isInstanceOf(WeatherException.class);
        }

        @Test
        @DisplayName("should throw for invalid longitude < -180")
        void shouldThrowForLongitudeBelow180() {
            assertThatThrownBy(() -> client.get5DayForecast(0, -181))
                    .isInstanceOf(WeatherException.class);
        }
    }

    @Nested
    @DisplayName("Climate zone simulation")
    class ClimateZoneSimulation {

        @Test
        @DisplayName("should return warmer temperatures for tropical locations")
        void shouldReturnWarmerTemperaturesForTropicalLocations() {
            // Singapore (tropical)
            List<WeatherForecast> tropicalForecasts = client.get5DayForecast(1.3521, 103.8198);

            double avgTropicalTemp = tropicalForecasts.stream()
                    .mapToDouble(WeatherForecast::getTempAvg)
                    .average()
                    .orElse(0);

            // Tropical should typically be above 20 C
            assertThat(avgTropicalTemp).isGreaterThan(15);
        }

        @Test
        @DisplayName("should return colder temperatures for polar locations")
        void shouldReturnColderTemperaturesForPolarLocations() {
            // Svalbard (high arctic)
            List<WeatherForecast> polarForecasts = client.get5DayForecast(78.2232, 15.6267);

            double avgPolarTemp = polarForecasts.stream()
                    .mapToDouble(WeatherForecast::getTempAvg)
                    .average()
                    .orElse(0);

            // Polar should be significantly colder than tropical average
            assertThat(avgPolarTemp).isLessThan(15);
        }
    }

    @Nested
    @DisplayName("WeatherForecast helper methods")
    class WeatherForecastHelperMethods {

        @Test
        @DisplayName("should return correct icon URL")
        void shouldReturnCorrectIconUrl() {
            List<WeatherForecast> forecasts = client.get5DayForecast(35.6812, 139.7671);
            WeatherForecast forecast = forecasts.get(0);

            String iconUrl = forecast.getIconUrl();

            assertThat(iconUrl).startsWith("https://openweathermap.org/img/wn/");
            assertThat(iconUrl).endsWith("@2x.png");
        }

        @Test
        @DisplayName("should return rain probability as percentage string")
        void shouldReturnRainProbabilityAsPercentage() {
            WeatherForecast forecast = WeatherForecast.builder()
                    .rainProbability(0.35)
                    .build();

            assertThat(forecast.getRainProbabilityPercent()).isEqualTo("35%");
        }

        @Test
        @DisplayName("should correctly identify rainy weather")
        void shouldCorrectlyIdentifyRainyWeather() {
            WeatherForecast rainy = WeatherForecast.builder()
                    .rainProbability(0.7)
                    .build();

            WeatherForecast notRainy = WeatherForecast.builder()
                    .rainProbability(0.3)
                    .build();

            assertThat(rainy.isRainy()).isTrue();
            assertThat(notRainy.isRainy()).isFalse();
        }
    }
}
