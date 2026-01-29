package com.wego.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.config.OpenWeatherMapProperties;
import com.wego.dto.response.WeatherForecast;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real implementation of WeatherClient that calls OpenWeatherMap 5-day forecast API.
 *
 * Uses the OpenWeatherMap 5-day/3-hour forecast API and aggregates data into daily forecasts.
 *
 * @contract
 *   - pre: OpenWeatherMapProperties must be configured with valid apiKey
 *   - post: All API calls include API key in request
 *   - throws: WeatherException on API errors or invalid responses
 *
 * @see WeatherClient
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "wego.external-api.openweathermap.enabled",
        havingValue = "true"
)
public class OpenWeatherMapClient implements WeatherClient {

    private static final String FORECAST_URL =
            "https://api.openweathermap.org/data/2.5/forecast";

    private final OpenWeatherMapProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Creates an OpenWeatherMapClient with the specified properties.
     *
     * @contract
     *   - pre: properties != null
     *   - post: Client is ready to make API calls with configured timeouts
     *
     * @param properties OpenWeatherMap configuration properties
     * @param restTemplate RestTemplate for HTTP calls
     */
    public OpenWeatherMapClient(OpenWeatherMapProperties properties, RestTemplate restTemplate) {
        if (properties == null) {
            throw new IllegalArgumentException("OpenWeatherMapProperties cannot be null");
        }
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Default constructor for Spring injection.
     * Configures RestTemplate with timeouts from properties.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public OpenWeatherMapClient(OpenWeatherMapProperties properties) {
        this(properties, createRestTemplateWithTimeouts(properties));
    }

    /**
     * Creates a RestTemplate with configured timeouts.
     */
    private static RestTemplate createRestTemplateWithTimeouts(OpenWeatherMapProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeoutMs());
        factory.setReadTimeout(properties.getReadTimeoutMs());
        return new RestTemplate(factory);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<WeatherForecast> get5DayForecast(double lat, double lng) {
        validateCoordinates(lat, lng);

        log.debug("Getting 5-day forecast for ({}, {})", lat, lng);

        String baseUrl = String.format(
                "%s?lat=%f&lon=%f&units=metric",
                FORECAST_URL,
                lat,
                lng
        );

        try {
            String url = appendApiKey(baseUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            String cod = root.path("cod").asText();
            if (!"200".equals(cod)) {
                String message = root.path("message").asText("Unknown error");
                handleApiError(cod, message);
            }

            List<WeatherForecast> forecasts = parseForecastResponse(root);

            log.info("Retrieved {} day forecasts for ({}, {})", forecasts.size(), lat, lng);
            return forecasts;

        } catch (WeatherException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("HTTP error getting weather forecast: {}", e.getMessage());
            throw WeatherException.networkError(e);
        } catch (Exception e) {
            log.error("Error getting weather forecast: {}", e.getMessage(), e);
            throw WeatherException.apiError("Failed to get weather forecast: " + e.getMessage());
        }
    }

    /**
     * Parses the OpenWeatherMap API response into daily forecasts.
     * The API returns 3-hour intervals, so we aggregate them into daily summaries.
     */
    private List<WeatherForecast> parseForecastResponse(JsonNode root) {
        JsonNode listNode = root.path("list");
        if (listNode.isEmpty()) {
            return new ArrayList<>();
        }

        // Group forecasts by date and aggregate
        Map<LocalDate, DailyAggregate> dailyData = new HashMap<>();

        for (JsonNode item : listNode) {
            long timestamp = item.path("dt").asLong();
            LocalDate date = Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            DailyAggregate aggregate = dailyData.computeIfAbsent(date, d -> new DailyAggregate());

            JsonNode mainNode = item.path("main");
            double temp = mainNode.path("temp").asDouble();
            double tempMin = mainNode.path("temp_min").asDouble();
            double tempMax = mainNode.path("temp_max").asDouble();
            int humidity = mainNode.path("humidity").asInt();

            aggregate.addTemperature(temp, tempMin, tempMax);
            aggregate.addHumidity(humidity);

            JsonNode weatherArray = item.path("weather");
            if (weatherArray.isArray() && weatherArray.size() > 0) {
                JsonNode weatherNode = weatherArray.get(0);
                aggregate.setCondition(weatherNode.path("main").asText());
                aggregate.setDescription(weatherNode.path("description").asText());
                aggregate.setIcon(weatherNode.path("icon").asText());
            }

            double pop = item.path("pop").asDouble(0);
            aggregate.addRainProbability(pop);

            JsonNode windNode = item.path("wind");
            if (!windNode.isMissingNode()) {
                aggregate.addWindSpeed(windNode.path("speed").asDouble());
            }
        }

        // Convert aggregates to forecasts
        List<WeatherForecast> forecasts = new ArrayList<>();
        List<LocalDate> sortedDates = new ArrayList<>(dailyData.keySet());
        sortedDates.sort(LocalDate::compareTo);

        for (LocalDate date : sortedDates) {
            DailyAggregate agg = dailyData.get(date);
            WeatherForecast forecast = WeatherForecast.builder()
                    .date(date)
                    .condition(agg.condition)
                    .description(agg.description)
                    .icon(agg.icon)
                    .tempHigh(agg.tempMax)
                    .tempLow(agg.tempMin)
                    .tempAvg(agg.getAverageTemp())
                    .humidity(agg.getAverageHumidity())
                    .rainProbability(agg.getMaxRainProbability())
                    .windSpeed(agg.getAverageWindSpeed())
                    .build();
            forecasts.add(forecast);
        }

        return forecasts;
    }

    /**
     * Helper class for aggregating 3-hour forecasts into daily summaries.
     */
    private static class DailyAggregate {
        double tempMin = Double.MAX_VALUE;
        double tempMax = Double.MIN_VALUE;
        double tempSum = 0;
        int tempCount = 0;
        int humiditySum = 0;
        int humidityCount = 0;
        double maxRainProbability = 0;
        double windSpeedSum = 0;
        int windCount = 0;
        String condition;
        String description;
        String icon;

        void addTemperature(double temp, double min, double max) {
            tempMin = Math.min(tempMin, min);
            tempMax = Math.max(tempMax, max);
            tempSum += temp;
            tempCount++;
        }

        void addHumidity(int humidity) {
            humiditySum += humidity;
            humidityCount++;
        }

        void addRainProbability(double pop) {
            maxRainProbability = Math.max(maxRainProbability, pop);
        }

        void addWindSpeed(double speed) {
            windSpeedSum += speed;
            windCount++;
        }

        void setCondition(String condition) {
            // Keep the most "significant" weather condition (prefer rain over clouds)
            if (this.condition == null || isMoreSignificant(condition)) {
                this.condition = condition;
            }
        }

        void setDescription(String description) {
            if (this.description == null) {
                this.description = description;
            }
        }

        void setIcon(String icon) {
            // Prefer daytime icons
            if (this.icon == null || icon.endsWith("d")) {
                this.icon = icon;
            }
        }

        private boolean isMoreSignificant(String newCondition) {
            // Weather significance order: Thunderstorm > Rain > Drizzle > Snow > Clouds > Clear
            Map<String, Integer> priority = Map.of(
                    "Thunderstorm", 6,
                    "Rain", 5,
                    "Drizzle", 4,
                    "Snow", 3,
                    "Clouds", 2,
                    "Clear", 1
            );
            int currentPriority = priority.getOrDefault(condition, 0);
            int newPriority = priority.getOrDefault(newCondition, 0);
            return newPriority > currentPriority;
        }

        double getAverageTemp() {
            return tempCount > 0 ? tempSum / tempCount : 0;
        }

        int getAverageHumidity() {
            return humidityCount > 0 ? humiditySum / humidityCount : 0;
        }

        double getMaxRainProbability() {
            return maxRainProbability;
        }

        double getAverageWindSpeed() {
            return windCount > 0 ? windSpeedSum / windCount : 0;
        }
    }

    /**
     * Handles API error responses.
     */
    private void handleApiError(String cod, String message) {
        log.error("OpenWeatherMap API error: cod={}, message={}", cod, message);

        switch (cod) {
            case "401":
                throw WeatherException.invalidApiKey();
            case "429":
                throw WeatherException.rateLimitExceeded();
            case "400":
                if (message.toLowerCase().contains("lat") || message.toLowerCase().contains("lon")) {
                    throw new WeatherException("INVALID_LOCATION", "Invalid coordinates: " + message);
                }
                throw WeatherException.apiError(message);
            default:
                throw WeatherException.apiError("API returned error: " + message);
        }
    }

    /**
     * Validates latitude and longitude values.
     */
    private void validateCoordinates(double lat, double lng) {
        if (lat < -90 || lat > 90) {
            throw WeatherException.invalidLocation(lat, lng);
        }
        if (lng < -180 || lng > 180) {
            throw WeatherException.invalidLocation(lat, lng);
        }
    }

    /**
     * Appends API key to URL.
     */
    private String appendApiKey(String baseUrl) {
        return baseUrl + "&appid=" + properties.getApiKey();
    }
}
