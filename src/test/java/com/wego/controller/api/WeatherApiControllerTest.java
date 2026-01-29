package com.wego.controller.api;

import com.wego.dto.response.WeatherForecast;
import com.wego.dto.response.WeatherResponse;
import com.wego.service.WeatherService;
import com.wego.service.external.WeatherException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for WeatherApiController.
 *
 * Tests weather API endpoints with mocked service layer.
 *
 * @see WeatherApiController
 */
@WebMvcTest(WeatherApiController.class)
@Import(TestSecurityConfig.class)
@DisplayName("WeatherApiController")
class WeatherApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WeatherService weatherService;

    private static final double TEST_LAT = 35.6812;
    private static final double TEST_LNG = 139.7671;

    private WeatherForecast createMockForecast(LocalDate date) {
        return WeatherForecast.builder()
                .date(date)
                .condition("Clear")
                .description("clear sky")
                .icon("01d")
                .tempHigh(25.0)
                .tempLow(18.0)
                .tempAvg(21.5)
                .humidity(55)
                .rainProbability(0.1)
                .windSpeed(3.5)
                .build();
    }

    private List<WeatherForecast> createMockForecasts() {
        LocalDate today = LocalDate.now();
        return Arrays.asList(
                createMockForecast(today),
                createMockForecast(today.plusDays(1)),
                createMockForecast(today.plusDays(2)),
                createMockForecast(today.plusDays(3)),
                createMockForecast(today.plusDays(4))
        );
    }

    @Nested
    @DisplayName("GET /api/weather")
    class GetWeather {

        @Test
        @DisplayName("should return weather for valid request")
        void shouldReturnWeatherForValidRequest() throws Exception {
            LocalDate today = LocalDate.now();
            WeatherForecast forecast = createMockForecast(today);
            WeatherResponse response = WeatherResponse.forDate(TEST_LAT, TEST_LNG, today, forecast);

            when(weatherService.getWeatherForDate(TEST_LAT, TEST_LNG, today))
                    .thenReturn(response);

            mockMvc.perform(get("/api/weather")
                            .param("lat", String.valueOf(TEST_LAT))
                            .param("lng", String.valueOf(TEST_LNG))
                            .param("date", today.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.available").value(true))
                    .andExpect(jsonPath("$.data.forecast.condition").value("Clear"))
                    .andExpect(jsonPath("$.data.forecast.tempHigh").value(25.0));
        }

        @Test
        @DisplayName("should return unavailable for date beyond range")
        void shouldReturnUnavailableForDateBeyondRange() throws Exception {
            LocalDate futureDate = LocalDate.now().plusDays(10);
            WeatherResponse response = WeatherResponse.notAvailable(TEST_LAT, TEST_LNG, futureDate);

            when(weatherService.getWeatherForDate(TEST_LAT, TEST_LNG, futureDate))
                    .thenReturn(response);

            mockMvc.perform(get("/api/weather")
                            .param("lat", String.valueOf(TEST_LAT))
                            .param("lng", String.valueOf(TEST_LNG))
                            .param("date", futureDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.available").value(false))
                    .andExpect(jsonPath("$.data.message").exists());
        }

        @Test
        @DisplayName("should return 400 for missing lat parameter")
        void shouldReturn400ForMissingLatParameter() throws Exception {
            mockMvc.perform(get("/api/weather")
                            .param("lng", String.valueOf(TEST_LNG))
                            .param("date", LocalDate.now().toString()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for missing lng parameter")
        void shouldReturn400ForMissingLngParameter() throws Exception {
            mockMvc.perform(get("/api/weather")
                            .param("lat", String.valueOf(TEST_LAT))
                            .param("date", LocalDate.now().toString()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for missing date parameter")
        void shouldReturn400ForMissingDateParameter() throws Exception {
            mockMvc.perform(get("/api/weather")
                            .param("lat", String.valueOf(TEST_LAT))
                            .param("lng", String.valueOf(TEST_LNG)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for invalid date format")
        void shouldReturn400ForInvalidDateFormat() throws Exception {
            mockMvc.perform(get("/api/weather")
                            .param("lat", String.valueOf(TEST_LAT))
                            .param("lng", String.valueOf(TEST_LNG))
                            .param("date", "invalid-date"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/weather/forecast")
    class GetFullForecast {

        @Test
        @DisplayName("should return full forecast for valid coordinates")
        void shouldReturnFullForecastForValidCoordinates() throws Exception {
            List<WeatherForecast> forecasts = createMockForecasts();
            WeatherResponse response = WeatherResponse.builder()
                    .latitude(TEST_LAT)
                    .longitude(TEST_LNG)
                    .available(true)
                    .forecasts(forecasts)
                    .build();

            when(weatherService.getFullForecast(TEST_LAT, TEST_LNG))
                    .thenReturn(response);

            mockMvc.perform(get("/api/weather/forecast")
                            .param("lat", String.valueOf(TEST_LAT))
                            .param("lng", String.valueOf(TEST_LNG)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.available").value(true))
                    .andExpect(jsonPath("$.data.forecasts").isArray())
                    .andExpect(jsonPath("$.data.forecasts.length()").value(5));
        }

        @Test
        @DisplayName("should return 400 for missing lat parameter")
        void shouldReturn400ForMissingLatParameter() throws Exception {
            mockMvc.perform(get("/api/weather/forecast")
                            .param("lng", String.valueOf(TEST_LNG)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 400 for missing lng parameter")
        void shouldReturn400ForMissingLngParameter() throws Exception {
            mockMvc.perform(get("/api/weather/forecast")
                            .param("lat", String.valueOf(TEST_LAT)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/weather/cache")
    class EvictCache {

        @Test
        @DisplayName("should evict cache successfully")
        void shouldEvictCacheSuccessfully() throws Exception {
            doNothing().when(weatherService).evictCache(TEST_LAT, TEST_LNG);

            mockMvc.perform(delete("/api/weather/cache")
                            .param("lat", String.valueOf(TEST_LAT))
                            .param("lng", String.valueOf(TEST_LNG)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Cache evicted successfully"));

            verify(weatherService).evictCache(TEST_LAT, TEST_LNG);
        }

        @Test
        @DisplayName("should return 400 for missing parameters")
        void shouldReturn400ForMissingParameters() throws Exception {
            mockMvc.perform(delete("/api/weather/cache")
                            .param("lat", String.valueOf(TEST_LAT)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("should handle WeatherException gracefully")
        void shouldHandleWeatherExceptionGracefully() throws Exception {
            when(weatherService.getWeatherForDate(anyDouble(), anyDouble(), any()))
                    .thenThrow(WeatherException.invalidLocation(100, 200));

            mockMvc.perform(get("/api/weather")
                            .param("lat", "100")
                            .param("lng", "200")
                            .param("date", LocalDate.now().toString()))
                    .andExpect(status().is5xxServerError());
        }
    }
}
