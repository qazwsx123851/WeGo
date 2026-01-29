package com.wego.service.external;

import com.wego.dto.response.WeatherForecast;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock implementation of WeatherClient for testing and development.
 * Generates realistic randomized weather data.
 *
 * @contract
 *   - Only active when openweathermap.enabled is false (default)
 *   - Uses seeded random based on coordinates for consistent results
 *   - Returns structurally valid data matching real API format
 *
 * @see WeatherClient
 */
@Slf4j
@Component
@ConditionalOnProperty(
        name = "wego.external-api.openweathermap.enabled",
        havingValue = "false",
        matchIfMissing = true
)
public class MockWeatherClient implements WeatherClient {

    // Weather condition types for mock data
    private static final String[] CONDITIONS = {
            "Clear", "Clouds", "Rain", "Drizzle", "Thunderstorm", "Snow"
    };

    private static final String[][] DESCRIPTIONS = {
            {"clear sky"},
            {"few clouds", "scattered clouds", "broken clouds", "overcast clouds"},
            {"light rain", "moderate rain", "heavy rain"},
            {"light drizzle", "drizzle"},
            {"thunderstorm", "thunderstorm with rain"},
            {"light snow", "snow"}
    };

    // Icon codes for each condition (day versions)
    private static final String[] ICONS = {
            "01d",  // Clear
            "03d",  // Clouds
            "10d",  // Rain
            "09d",  // Drizzle
            "11d",  // Thunderstorm
            "13d"   // Snow
    };

    /**
     * {@inheritDoc}
     *
     * Generates mock weather data based on location coordinates.
     * Uses coordinate hash for consistent results with the same inputs.
     */
    @Override
    public List<WeatherForecast> get5DayForecast(double lat, double lng) {
        validateCoordinates(lat, lng);

        log.debug("[MOCK] Getting 5-day forecast for ({}, {})", lat, lng);

        // Use coordinates to seed random for consistent results
        long seed = Double.hashCode(lat) * 31L + Double.hashCode(lng);
        Random random = new Random(seed);

        List<WeatherForecast> forecasts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Generate base temperature based on latitude (rough climate zones)
        double baseTemp = calculateBaseTemperature(lat, random);

        for (int day = 0; day < 5; day++) {
            LocalDate date = today.plusDays(day);

            // Add some daily variation
            double dailyVariation = (random.nextDouble() - 0.5) * 10;
            double tempAvg = baseTemp + dailyVariation;
            double tempRange = 5 + random.nextDouble() * 5;
            double tempHigh = tempAvg + tempRange / 2;
            double tempLow = tempAvg - tempRange / 2;

            // Select weather condition
            int conditionIndex = selectWeatherCondition(random, lat);
            String condition = CONDITIONS[conditionIndex];
            String[] possibleDescriptions = DESCRIPTIONS[conditionIndex];
            String description = possibleDescriptions[random.nextInt(possibleDescriptions.length)];
            String icon = ICONS[conditionIndex];

            // Calculate rain probability based on condition
            double rainProbability = calculateRainProbability(conditionIndex, random);

            // Generate other values
            int humidity = 40 + random.nextInt(40); // 40-80%
            double windSpeed = 1 + random.nextDouble() * 10; // 1-11 m/s

            WeatherForecast forecast = WeatherForecast.builder()
                    .date(date)
                    .condition(condition)
                    .description(description)
                    .icon(icon)
                    .tempHigh(Math.round(tempHigh * 10) / 10.0)
                    .tempLow(Math.round(tempLow * 10) / 10.0)
                    .tempAvg(Math.round(tempAvg * 10) / 10.0)
                    .humidity(humidity)
                    .rainProbability(Math.round(rainProbability * 100) / 100.0)
                    .windSpeed(Math.round(windSpeed * 10) / 10.0)
                    .build();

            forecasts.add(forecast);
        }

        log.info("[MOCK] Generated {} day forecasts for ({}, {})", forecasts.size(), lat, lng);
        return forecasts;
    }

    /**
     * Calculates base temperature based on latitude.
     * Simple climate model: warmer near equator, colder near poles.
     */
    private double calculateBaseTemperature(double lat, Random random) {
        double absLat = Math.abs(lat);

        if (absLat < 23.5) {
            // Tropical zone: 25-35 C
            return 28 + (random.nextDouble() - 0.5) * 10;
        } else if (absLat < 45) {
            // Temperate zone: 10-25 C
            return 18 + (random.nextDouble() - 0.5) * 15;
        } else if (absLat < 66.5) {
            // Cold temperate zone: 0-15 C
            return 8 + (random.nextDouble() - 0.5) * 15;
        } else {
            // Polar zone: -10 to 5 C
            return -3 + (random.nextDouble() - 0.5) * 15;
        }
    }

    /**
     * Selects a weather condition with realistic probability distribution.
     * Higher latitude = more chance of clouds/precipitation.
     */
    private int selectWeatherCondition(Random random, double lat) {
        double absLat = Math.abs(lat);
        double rand = random.nextDouble();

        // Base probabilities: Clear 40%, Clouds 30%, Rain 15%, Drizzle 10%, Thunder 4%, Snow 1%
        double clearProb = 0.40 - absLat * 0.002;  // Less clear at high latitudes
        double cloudsProb = clearProb + 0.30;
        double rainProb = cloudsProb + 0.15;
        double drizzleProb = rainProb + 0.10;
        double thunderProb = drizzleProb + 0.04;

        if (rand < clearProb) {
            return 0; // Clear
        } else if (rand < cloudsProb) {
            return 1; // Clouds
        } else if (rand < rainProb) {
            return 2; // Rain
        } else if (rand < drizzleProb) {
            return 3; // Drizzle
        } else if (rand < thunderProb) {
            return 4; // Thunderstorm
        } else {
            // Snow only at high latitudes
            return absLat > 35 ? 5 : 2; // Snow or Rain
        }
    }

    /**
     * Calculates rain probability based on weather condition.
     */
    private double calculateRainProbability(int conditionIndex, Random random) {
        return switch (conditionIndex) {
            case 0 -> 0.0 + random.nextDouble() * 0.1;        // Clear: 0-10%
            case 1 -> 0.1 + random.nextDouble() * 0.3;        // Clouds: 10-40%
            case 2 -> 0.7 + random.nextDouble() * 0.3;        // Rain: 70-100%
            case 3 -> 0.6 + random.nextDouble() * 0.3;        // Drizzle: 60-90%
            case 4 -> 0.8 + random.nextDouble() * 0.2;        // Thunderstorm: 80-100%
            case 5 -> 0.5 + random.nextDouble() * 0.4;        // Snow: 50-90%
            default -> 0.2;
        };
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
}
