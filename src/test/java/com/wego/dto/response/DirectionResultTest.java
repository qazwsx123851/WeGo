package com.wego.dto.response;

import com.wego.entity.TransportMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DirectionResult DTO.
 *
 * @see DirectionResult
 */
@DisplayName("DirectionResult")
class DirectionResultTest {

    @Nested
    @DisplayName("Builder")
    class Builder {

        @Test
        @DisplayName("should create DirectionResult with all fields")
        void shouldCreateWithAllFields() {
            DirectionResult result = DirectionResult.builder()
                    .originAddress("Tokyo Station")
                    .destinationAddress("Shibuya Station")
                    .distanceMeters(5000)
                    .distanceText("5.0 km")
                    .durationSeconds(900)
                    .durationText("15 mins")
                    .transportMode(TransportMode.TRANSIT)
                    .build();

            assertThat(result.getOriginAddress()).isEqualTo("Tokyo Station");
            assertThat(result.getDestinationAddress()).isEqualTo("Shibuya Station");
            assertThat(result.getDistanceMeters()).isEqualTo(5000);
            assertThat(result.getDistanceText()).isEqualTo("5.0 km");
            assertThat(result.getDurationSeconds()).isEqualTo(900);
            assertThat(result.getDurationText()).isEqualTo("15 mins");
            assertThat(result.getTransportMode()).isEqualTo(TransportMode.TRANSIT);
        }

        @Test
        @DisplayName("should create DirectionResult with minimal fields")
        void shouldCreateWithMinimalFields() {
            DirectionResult result = DirectionResult.builder()
                    .distanceMeters(1000)
                    .durationSeconds(600)
                    .build();

            assertThat(result.getDistanceMeters()).isEqualTo(1000);
            assertThat(result.getDurationSeconds()).isEqualTo(600);
            assertThat(result.getOriginAddress()).isNull();
            assertThat(result.getDestinationAddress()).isNull();
        }
    }

    @Nested
    @DisplayName("All Args Constructor")
    class AllArgsConstructor {

        @Test
        @DisplayName("should create DirectionResult with all args using builder")
        void shouldCreateWithAllArgs() {
            // Use builder pattern instead of all-args constructor for maintainability
            DirectionResult result = DirectionResult.builder()
                    .originAddress("Origin")
                    .destinationAddress("Destination")
                    .distanceMeters(2000)
                    .distanceText("2.0 km")
                    .durationSeconds(300)
                    .durationText("5 mins")
                    .transportMode(TransportMode.WALKING)
                    .apiSource(DirectionResult.ApiSource.DISTANCE_MATRIX)
                    .fromFallback(false)
                    .build();

            assertThat(result.getOriginAddress()).isEqualTo("Origin");
            assertThat(result.getDestinationAddress()).isEqualTo("Destination");
            assertThat(result.getDistanceMeters()).isEqualTo(2000);
            assertThat(result.getDistanceText()).isEqualTo("2.0 km");
            assertThat(result.getDurationSeconds()).isEqualTo(300);
            assertThat(result.getDurationText()).isEqualTo("5 mins");
            assertThat(result.getTransportMode()).isEqualTo(TransportMode.WALKING);
            assertThat(result.getApiSource()).isEqualTo(DirectionResult.ApiSource.DISTANCE_MATRIX);
            assertThat(result.isFromFallback()).isFalse();
        }
    }

    @Nested
    @DisplayName("No Args Constructor")
    class NoArgsConstructor {

        @Test
        @DisplayName("should create empty DirectionResult")
        void shouldCreateEmptyResult() {
            DirectionResult result = new DirectionResult();

            assertThat(result.getOriginAddress()).isNull();
            assertThat(result.getDistanceMeters()).isZero();
            assertThat(result.getDurationSeconds()).isZero();
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("should set all fields via setters")
        void shouldSetAllFieldsViaSetters() {
            DirectionResult result = new DirectionResult();
            result.setOriginAddress("Start");
            result.setDestinationAddress("End");
            result.setDistanceMeters(3000);
            result.setDistanceText("3.0 km");
            result.setDurationSeconds(600);
            result.setDurationText("10 mins");
            result.setTransportMode(TransportMode.DRIVING);

            assertThat(result.getOriginAddress()).isEqualTo("Start");
            assertThat(result.getDestinationAddress()).isEqualTo("End");
            assertThat(result.getDistanceMeters()).isEqualTo(3000);
            assertThat(result.getDistanceText()).isEqualTo("3.0 km");
            assertThat(result.getDurationSeconds()).isEqualTo(600);
            assertThat(result.getDurationText()).isEqualTo("10 mins");
            assertThat(result.getTransportMode()).isEqualTo(TransportMode.DRIVING);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        @DisplayName("should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            DirectionResult result1 = DirectionResult.builder()
                    .distanceMeters(1000)
                    .durationSeconds(300)
                    .transportMode(TransportMode.WALKING)
                    .build();

            DirectionResult result2 = DirectionResult.builder()
                    .distanceMeters(1000)
                    .durationSeconds(300)
                    .transportMode(TransportMode.WALKING)
                    .build();

            assertThat(result1).isEqualTo(result2);
            assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
        }

        @Test
        @DisplayName("should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            DirectionResult result1 = DirectionResult.builder()
                    .distanceMeters(1000)
                    .build();

            DirectionResult result2 = DirectionResult.builder()
                    .distanceMeters(2000)
                    .build();

            assertThat(result1).isNotEqualTo(result2);
        }
    }

    @Nested
    @DisplayName("ToString")
    class ToString {

        @Test
        @DisplayName("should return readable string representation")
        void shouldReturnReadableString() {
            DirectionResult result = DirectionResult.builder()
                    .originAddress("Tokyo")
                    .destinationAddress("Osaka")
                    .distanceMeters(500000)
                    .durationSeconds(10800)
                    .build();

            String toString = result.toString();

            assertThat(toString).contains("Tokyo");
            assertThat(toString).contains("Osaka");
            assertThat(toString).contains("500000");
            assertThat(toString).contains("10800");
        }
    }
}
