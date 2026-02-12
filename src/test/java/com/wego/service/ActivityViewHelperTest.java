package com.wego.service;

import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.TransportMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActivityViewHelperTest {

    private final ActivityViewHelper helper = new ActivityViewHelper();

    @Nested
    @DisplayName("groupActivitiesByDate")
    class GroupActivitiesByDate {

        @Test
        @DisplayName("empty activities list returns map with all dates but empty lists")
        void emptyActivitiesList() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.of(2025, 1, 1))
                    .endDate(LocalDate.of(2025, 1, 3))
                    .build();

            Map<LocalDate, List<ActivityResponse>> result =
                    helper.groupActivitiesByDate(trip, Collections.emptyList());

            assertThat(result).hasSize(3);
            assertThat(result.get(LocalDate.of(2025, 1, 1))).isEmpty();
            assertThat(result.get(LocalDate.of(2025, 1, 2))).isEmpty();
            assertThat(result.get(LocalDate.of(2025, 1, 3))).isEmpty();
        }

        @Test
        @DisplayName("single day trip with activities groups correctly")
        void singleDayTripWithActivities() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.of(2025, 3, 15))
                    .endDate(LocalDate.of(2025, 3, 15))
                    .build();

            ActivityResponse a1 = ActivityResponse.builder().day(1).sortOrder(1).build();
            ActivityResponse a2 = ActivityResponse.builder().day(1).sortOrder(2).build();

            Map<LocalDate, List<ActivityResponse>> result =
                    helper.groupActivitiesByDate(trip, List.of(a1, a2));

            assertThat(result).hasSize(1);
            assertThat(result.get(LocalDate.of(2025, 3, 15))).containsExactly(a1, a2);
        }

        @Test
        @DisplayName("multi-day trip distributes activities to correct dates")
        void multiDayTrip() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.of(2025, 6, 1))
                    .endDate(LocalDate.of(2025, 6, 3))
                    .build();

            ActivityResponse day1Activity = ActivityResponse.builder().day(1).sortOrder(1).build();
            ActivityResponse day2Activity = ActivityResponse.builder().day(2).sortOrder(1).build();
            ActivityResponse day3Activity = ActivityResponse.builder().day(3).sortOrder(1).build();

            Map<LocalDate, List<ActivityResponse>> result =
                    helper.groupActivitiesByDate(trip, List.of(day1Activity, day2Activity, day3Activity));

            assertThat(result).hasSize(3);
            assertThat(result.get(LocalDate.of(2025, 6, 1))).containsExactly(day1Activity);
            assertThat(result.get(LocalDate.of(2025, 6, 2))).containsExactly(day2Activity);
            assertThat(result.get(LocalDate.of(2025, 6, 3))).containsExactly(day3Activity);
        }

        @Test
        @DisplayName("activity with day=0 is excluded")
        void dayZeroExcluded() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.of(2025, 1, 1))
                    .endDate(LocalDate.of(2025, 1, 2))
                    .build();

            ActivityResponse invalidActivity = ActivityResponse.builder().day(0).sortOrder(1).build();

            Map<LocalDate, List<ActivityResponse>> result =
                    helper.groupActivitiesByDate(trip, List.of(invalidActivity));

            assertThat(result.get(LocalDate.of(2025, 1, 1))).isEmpty();
            assertThat(result.get(LocalDate.of(2025, 1, 2))).isEmpty();
        }

        @Test
        @DisplayName("activity with day exceeding total days is excluded")
        void dayExceedingTotalDaysExcluded() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.of(2025, 1, 1))
                    .endDate(LocalDate.of(2025, 1, 2))
                    .build();

            ActivityResponse invalidActivity = ActivityResponse.builder().day(5).sortOrder(1).build();

            Map<LocalDate, List<ActivityResponse>> result =
                    helper.groupActivitiesByDate(trip, List.of(invalidActivity));

            assertThat(result.get(LocalDate.of(2025, 1, 1))).isEmpty();
            assertThat(result.get(LocalDate.of(2025, 1, 2))).isEmpty();
        }

        @Test
        @DisplayName("activities within same day sorted by sortOrder")
        void sortedBySortOrder() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.of(2025, 1, 1))
                    .endDate(LocalDate.of(2025, 1, 1))
                    .build();

            ActivityResponse a3 = ActivityResponse.builder().day(1).sortOrder(3).build();
            ActivityResponse a1 = ActivityResponse.builder().day(1).sortOrder(1).build();
            ActivityResponse a2 = ActivityResponse.builder().day(1).sortOrder(2).build();

            Map<LocalDate, List<ActivityResponse>> result =
                    helper.groupActivitiesByDate(trip, List.of(a3, a1, a2));

            List<ActivityResponse> dayActivities = result.get(LocalDate.of(2025, 1, 1));
            assertThat(dayActivities).containsExactly(a1, a2, a3);
        }
    }

    @Nested
    @DisplayName("generateTripDates")
    class GenerateTripDates {

        @Test
        @DisplayName("normal range returns all dates inclusive")
        void normalRange() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.of(2025, 1, 1))
                    .endDate(LocalDate.of(2025, 1, 3))
                    .build();

            List<LocalDate> result = helper.generateTripDates(trip);

            assertThat(result).containsExactly(
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 2),
                    LocalDate.of(2025, 1, 3)
            );
        }

        @Test
        @DisplayName("same start and end date returns single date")
        void sameStartAndEnd() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.of(2025, 5, 10))
                    .endDate(LocalDate.of(2025, 5, 10))
                    .build();

            List<LocalDate> result = helper.generateTripDates(trip);

            assertThat(result).containsExactly(LocalDate.of(2025, 5, 10));
        }

        @Test
        @DisplayName("null startDate returns list with today")
        void nullStartDate() {
            TripResponse trip = TripResponse.builder()
                    .startDate(null)
                    .endDate(LocalDate.of(2025, 1, 3))
                    .build();

            List<LocalDate> result = helper.generateTripDates(trip);

            assertThat(result).containsExactly(LocalDate.now());
        }

        @Test
        @DisplayName("null endDate returns list with today")
        void nullEndDate() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.of(2025, 1, 1))
                    .endDate(null)
                    .build();

            List<LocalDate> result = helper.generateTripDates(trip);

            assertThat(result).containsExactly(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("validateTransportInput")
    class ValidateTransportInput {

        @Test
        @DisplayName("valid mode WALKING returns WALKING with isValid=true")
        void validWalkingMode() {
            ActivityViewHelper.TransportValidationResult result =
                    helper.validateTransportInput("WALKING", null);

            assertThat(result.transportMode()).isEqualTo(TransportMode.WALKING);
            assertThat(result.isValid()).isTrue();
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("invalid mode defaults to WALKING with isValid=true")
        void invalidModeDefaultsToWalking() {
            ActivityViewHelper.TransportValidationResult result =
                    helper.validateTransportInput("INVALID", null);

            assertThat(result.transportMode()).isEqualTo(TransportMode.WALKING);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("FLIGHT with manualMinutes=120 returns FLIGHT with 120, isValid=true")
        void flightWithValidMinutes() {
            ActivityViewHelper.TransportValidationResult result =
                    helper.validateTransportInput("FLIGHT", 120);

            assertThat(result.transportMode()).isEqualTo(TransportMode.FLIGHT);
            assertThat(result.manualMinutes()).isEqualTo(120);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("FLIGHT with null manualMinutes returns error, isValid=false")
        void flightWithNullMinutes() {
            ActivityViewHelper.TransportValidationResult result =
                    helper.validateTransportInput("FLIGHT", null);

            assertThat(result.transportMode()).isEqualTo(TransportMode.FLIGHT);
            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).isNotEmpty();
        }

        @Test
        @DisplayName("HIGH_SPEED_RAIL with minutes=0 returns error, isValid=false")
        void highSpeedRailWithZeroMinutes() {
            ActivityViewHelper.TransportValidationResult result =
                    helper.validateTransportInput("HIGH_SPEED_RAIL", 0);

            assertThat(result.transportMode()).isEqualTo(TransportMode.HIGH_SPEED_RAIL);
            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).isNotEmpty();
        }

        @Test
        @DisplayName("manual minutes < 0 set to null")
        void negativeMinutesSetToNull() {
            ActivityViewHelper.TransportValidationResult result =
                    helper.validateTransportInput("WALKING", -5);

            assertThat(result.manualMinutes()).isNull();
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("manual minutes > 2880 capped to 2880")
        void minutesCappedTo2880() {
            ActivityViewHelper.TransportValidationResult result =
                    helper.validateTransportInput("WALKING", 5000);

            assertThat(result.manualMinutes()).isEqualTo(2880);
            assertThat(result.isValid()).isTrue();
        }
    }
}
