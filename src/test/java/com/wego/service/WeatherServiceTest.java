package com.wego.service;

import com.wego.dto.response.WeatherForecast;
import com.wego.dto.response.WeatherResponse;
import com.wego.service.external.MockWeatherClient;
import com.wego.service.external.WeatherClient;
import com.wego.service.external.WeatherException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WeatherService.
 *
 * Tests weather forecast retrieval, caching, and date validation.
 *
 * @see WeatherService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherService")
class WeatherServiceTest {

    @Mock
    private WeatherClient weatherClient;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private WeatherService weatherService;

    private static final double TEST_LAT = 35.6812;
    private static final double TEST_LNG = 139.7671;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(weatherClient, cacheManager);
    }

    private List<WeatherForecast> createMockForecasts() {
        LocalDate today = LocalDate.now();
        return Arrays.asList(
                WeatherForecast.builder()
                        .date(today)
                        .condition("Clear")
                        .description("clear sky")
                        .icon("01d")
                        .tempHigh(25.0)
                        .tempLow(18.0)
                        .tempAvg(21.5)
                        .humidity(55)
                        .rainProbability(0.1)
                        .windSpeed(3.5)
                        .build(),
                WeatherForecast.builder()
                        .date(today.plusDays(1))
                        .condition("Clouds")
                        .description("scattered clouds")
                        .icon("03d")
                        .tempHigh(23.0)
                        .tempLow(17.0)
                        .tempAvg(20.0)
                        .humidity(60)
                        .rainProbability(0.3)
                        .windSpeed(4.0)
                        .build(),
                WeatherForecast.builder()
                        .date(today.plusDays(2))
                        .condition("Rain")
                        .description("light rain")
                        .icon("10d")
                        .tempHigh(20.0)
                        .tempLow(15.0)
                        .tempAvg(17.5)
                        .humidity(80)
                        .rainProbability(0.8)
                        .windSpeed(5.5)
                        .build(),
                WeatherForecast.builder()
                        .date(today.plusDays(3))
                        .condition("Clouds")
                        .description("broken clouds")
                        .icon("04d")
                        .tempHigh(22.0)
                        .tempLow(16.0)
                        .tempAvg(19.0)
                        .humidity(65)
                        .rainProbability(0.4)
                        .windSpeed(3.0)
                        .build(),
                WeatherForecast.builder()
                        .date(today.plusDays(4))
                        .condition("Clear")
                        .description("clear sky")
                        .icon("01d")
                        .tempHigh(26.0)
                        .tempLow(19.0)
                        .tempAvg(22.5)
                        .humidity(50)
                        .rainProbability(0.05)
                        .windSpeed(2.5)
                        .build()
        );
    }

    private void setupCacheMiss() {
        when(cacheManager.getCache("weather")).thenReturn(cache);
        when(cache.get(anyString())).thenReturn(null);
    }

    private void setupCacheHit(List<WeatherForecast> forecasts) {
        when(cacheManager.getCache("weather")).thenReturn(cache);
        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);
        when(wrapper.get()).thenReturn(forecasts);
        when(cache.get(anyString())).thenReturn(wrapper);
    }

    @Nested
    @DisplayName("getWeatherForDate")
    class GetWeatherForDate {

        @Test
        @DisplayName("should return weather for valid date within range")
        void shouldReturnWeatherForValidDate() {
            LocalDate today = LocalDate.now();
            List<WeatherForecast> mockForecasts = createMockForecasts();

            setupCacheMiss();
            when(weatherClient.get5DayForecast(TEST_LAT, TEST_LNG))
                    .thenReturn(mockForecasts);

            WeatherResponse response = weatherService.getWeatherForDate(TEST_LAT, TEST_LNG, today);

            assertThat(response).isNotNull();
            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getForecast()).isNotNull();
            assertThat(response.getForecast().getDate()).isEqualTo(today);
            assertThat(response.getForecast().getCondition()).isEqualTo("Clear");
        }

        @Test
        @DisplayName("should return unavailable for date beyond 5 days")
        void shouldReturnUnavailableForDateBeyondRange() {
            LocalDate futureDate = LocalDate.now().plusDays(10);

            WeatherResponse response = weatherService.getWeatherForDate(TEST_LAT, TEST_LNG, futureDate);

            assertThat(response).isNotNull();
            assertThat(response.isAvailable()).isFalse();
            assertThat(response.getMessage()).contains("not available");
            assertThat(response.getForecast()).isNull();

            verify(weatherClient, never()).get5DayForecast(anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("should return unavailable for past dates")
        void shouldReturnUnavailableForPastDates() {
            LocalDate pastDate = LocalDate.now().minusDays(1);

            WeatherResponse response = weatherService.getWeatherForDate(TEST_LAT, TEST_LNG, pastDate);

            assertThat(response).isNotNull();
            assertThat(response.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should use cached data when available")
        void shouldUseCachedData() {
            LocalDate today = LocalDate.now();
            List<WeatherForecast> cachedForecasts = createMockForecasts();

            setupCacheHit(cachedForecasts);

            WeatherResponse response = weatherService.getWeatherForDate(TEST_LAT, TEST_LNG, today);

            assertThat(response).isNotNull();
            assertThat(response.isAvailable()).isTrue();

            verify(weatherClient, never()).get5DayForecast(anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("should fetch from API on cache miss")
        void shouldFetchFromApiOnCacheMiss() {
            LocalDate today = LocalDate.now();
            List<WeatherForecast> mockForecasts = createMockForecasts();

            setupCacheMiss();
            when(weatherClient.get5DayForecast(TEST_LAT, TEST_LNG))
                    .thenReturn(mockForecasts);

            weatherService.getWeatherForDate(TEST_LAT, TEST_LNG, today);

            verify(weatherClient).get5DayForecast(TEST_LAT, TEST_LNG);
            verify(cache).put(anyString(), eq(mockForecasts));
        }

        @Test
        @DisplayName("should throw for invalid latitude")
        void shouldThrowForInvalidLatitude() {
            assertThatThrownBy(() ->
                    weatherService.getWeatherForDate(100.0, TEST_LNG, LocalDate.now()))
                    .isInstanceOf(WeatherException.class);
        }

        @Test
        @DisplayName("should throw for invalid longitude")
        void shouldThrowForInvalidLongitude() {
            assertThatThrownBy(() ->
                    weatherService.getWeatherForDate(TEST_LAT, 200.0, LocalDate.now()))
                    .isInstanceOf(WeatherException.class);
        }
    }

    @Nested
    @DisplayName("getFullForecast")
    class GetFullForecast {

        @Test
        @DisplayName("should return all forecasts")
        void shouldReturnAllForecasts() {
            List<WeatherForecast> mockForecasts = createMockForecasts();

            setupCacheMiss();
            when(weatherClient.get5DayForecast(TEST_LAT, TEST_LNG))
                    .thenReturn(mockForecasts);

            WeatherResponse response = weatherService.getFullForecast(TEST_LAT, TEST_LNG);

            assertThat(response).isNotNull();
            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getForecasts()).hasSize(5);
            assertThat(response.getLatitude()).isEqualTo(TEST_LAT);
            assertThat(response.getLongitude()).isEqualTo(TEST_LNG);
        }

        @Test
        @DisplayName("should cache forecasts after fetching")
        void shouldCacheForecastsAfterFetching() {
            List<WeatherForecast> mockForecasts = createMockForecasts();

            setupCacheMiss();
            when(weatherClient.get5DayForecast(TEST_LAT, TEST_LNG))
                    .thenReturn(mockForecasts);

            weatherService.getFullForecast(TEST_LAT, TEST_LNG);

            verify(cache).put(
                    contains("forecast:"),
                    eq(mockForecasts)
            );
        }
    }

    @Nested
    @DisplayName("Cache operations")
    class CacheOperations {

        @Test
        @DisplayName("should evict cache for specific location")
        void shouldEvictCacheForLocation() {
            when(cacheManager.getCache("weather")).thenReturn(cache);

            weatherService.evictCache(TEST_LAT, TEST_LNG);

            verify(cache).evict(contains("forecast:"));
        }

        @Test
        @DisplayName("should evict all cache entries")
        void shouldEvictAllCacheEntries() {
            when(cacheManager.getCache("weather")).thenReturn(cache);

            weatherService.evictAllCache();

            verify(cache).clear();
        }

        @Test
        @DisplayName("should use consistent cache key for same coordinates")
        void shouldUseConsistentCacheKey() {
            List<WeatherForecast> mockForecasts = createMockForecasts();

            setupCacheMiss();
            when(weatherClient.get5DayForecast(TEST_LAT, TEST_LNG))
                    .thenReturn(mockForecasts);

            weatherService.getFullForecast(TEST_LAT, TEST_LNG);

            verify(weatherClient, times(1)).get5DayForecast(TEST_LAT, TEST_LNG);

            verify(cache, times(1)).put(
                    eq("forecast:35.68:139.77"),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("Coordinate validation")
    class CoordinateValidation {

        @Test
        @DisplayName("should accept valid coordinates")
        void shouldAcceptValidCoordinates() {
            List<WeatherForecast> mockForecasts = createMockForecasts();

            when(cacheManager.getCache("weather")).thenReturn(cache);
            when(cache.get(anyString())).thenReturn(null);
            when(weatherClient.get5DayForecast(anyDouble(), anyDouble()))
                    .thenReturn(mockForecasts);

            assertThat(weatherService.getFullForecast(0, 0)).isNotNull();
            assertThat(weatherService.getFullForecast(-90, -180)).isNotNull();
            assertThat(weatherService.getFullForecast(90, 180)).isNotNull();
        }

        @Test
        @DisplayName("should reject latitude > 90")
        void shouldRejectLatitudeAbove90() {
            assertThatThrownBy(() -> weatherService.getFullForecast(91, 0))
                    .isInstanceOf(WeatherException.class);
        }

        @Test
        @DisplayName("should reject latitude < -90")
        void shouldRejectLatitudeBelow90() {
            assertThatThrownBy(() -> weatherService.getFullForecast(-91, 0))
                    .isInstanceOf(WeatherException.class);
        }

        @Test
        @DisplayName("should reject longitude > 180")
        void shouldRejectLongitudeAbove180() {
            assertThatThrownBy(() -> weatherService.getFullForecast(0, 181))
                    .isInstanceOf(WeatherException.class);
        }

        @Test
        @DisplayName("should reject longitude < -180")
        void shouldRejectLongitudeBelow180() {
            assertThatThrownBy(() -> weatherService.getFullForecast(0, -181))
                    .isInstanceOf(WeatherException.class);
        }
    }

    @Nested
    @DisplayName("Date range validation")
    class DateRangeValidation {

        @Test
        @DisplayName("should accept today")
        void shouldAcceptToday() {
            List<WeatherForecast> mockForecasts = createMockForecasts();

            setupCacheHit(mockForecasts);

            WeatherResponse response = weatherService.getWeatherForDate(
                    TEST_LAT, TEST_LNG, LocalDate.now());

            assertThat(response.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("should accept day 4 from today")
        void shouldAcceptDay4FromToday() {
            List<WeatherForecast> mockForecasts = createMockForecasts();

            setupCacheHit(mockForecasts);

            WeatherResponse response = weatherService.getWeatherForDate(
                    TEST_LAT, TEST_LNG, LocalDate.now().plusDays(4));

            assertThat(response.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("should reject day 5 from today")
        void shouldRejectDay5FromToday() {
            WeatherResponse response = weatherService.getWeatherForDate(
                    TEST_LAT, TEST_LNG, LocalDate.now().plusDays(5));

            assertThat(response.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("should reject yesterday")
        void shouldRejectYesterday() {
            WeatherResponse response = weatherService.getWeatherForDate(
                    TEST_LAT, TEST_LNG, LocalDate.now().minusDays(1));

            assertThat(response.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Integration with MockWeatherClient")
    class MockClientIntegration {

        @Test
        @DisplayName("should work with MockWeatherClient")
        void shouldWorkWithMockClient() {
            MockWeatherClient mockClient = new MockWeatherClient();
            CacheManager realCacheManager = new ConcurrentMapCacheManager("weather");
            WeatherService serviceWithMock = new WeatherService(mockClient, realCacheManager);

            WeatherResponse response = serviceWithMock.getFullForecast(
                    35.6812, 139.7671); // Tokyo

            assertThat(response).isNotNull();
            assertThat(response.isAvailable()).isTrue();
            assertThat(response.getForecasts()).hasSize(5);

            for (WeatherForecast forecast : response.getForecasts()) {
                assertThat(forecast.getDate()).isNotNull();
                assertThat(forecast.getCondition()).isNotNull();
                assertThat(forecast.getIcon()).isNotNull();
                assertThat(forecast.getIconUrl()).startsWith("https://");
            }
        }

        @Test
        @DisplayName("should return consistent results for same coordinates")
        void shouldReturnConsistentResultsForSameCoordinates() {
            MockWeatherClient mockClient = new MockWeatherClient();

            List<WeatherForecast> forecast1 = mockClient.get5DayForecast(35.6812, 139.7671);
            List<WeatherForecast> forecast2 = mockClient.get5DayForecast(35.6812, 139.7671);

            assertThat(forecast1).hasSize(forecast2.size());
            for (int i = 0; i < forecast1.size(); i++) {
                assertThat(forecast1.get(i).getTempHigh())
                        .isEqualTo(forecast2.get(i).getTempHigh());
                assertThat(forecast1.get(i).getCondition())
                        .isEqualTo(forecast2.get(i).getCondition());
            }
        }
    }
}
