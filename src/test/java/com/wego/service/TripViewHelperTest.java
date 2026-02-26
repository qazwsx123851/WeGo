package com.wego.service;

import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.PlaceResponse;
import com.wego.dto.response.TodoResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.TodoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TripViewHelper.
 * Tests view-preparation logic extracted from controllers.
 */
@ExtendWith(MockitoExtension.class)
class TripViewHelperTest {

    @Mock
    private ActivityService activityService;

    @Mock
    private TodoService todoService;

    @Mock
    private ExpenseService expenseService;

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private TripViewHelper tripViewHelper;

    private UUID tripId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("calculateDaysUntilMap")
    class CalculateDaysUntilMap {

        @Test
        @DisplayName("should return empty map for empty trip list")
        void calculateDaysUntilMap_emptyList_shouldReturnEmpty() {
            Map<UUID, Long> result = tripViewHelper.calculateDaysUntilMap(List.of());
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should include future trips with correct days")
        void calculateDaysUntilMap_futureTrips_shouldCalculateDays() {
            LocalDate futureDate = LocalDate.now().plusDays(10);
            TripResponse trip = TripResponse.builder()
                    .id(tripId)
                    .startDate(futureDate)
                    .build();

            Map<UUID, Long> result = tripViewHelper.calculateDaysUntilMap(List.of(trip));

            assertThat(result).containsKey(tripId);
            assertThat(result.get(tripId)).isEqualTo(10L);
        }

        @Test
        @DisplayName("should exclude past trips")
        void calculateDaysUntilMap_pastTrips_shouldExclude() {
            LocalDate pastDate = LocalDate.now().minusDays(5);
            TripResponse trip = TripResponse.builder()
                    .id(tripId)
                    .startDate(pastDate)
                    .build();

            Map<UUID, Long> result = tripViewHelper.calculateDaysUntilMap(List.of(trip));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should include today's trip with 0 days")
        void calculateDaysUntilMap_todayTrip_shouldBeZero() {
            TripResponse trip = TripResponse.builder()
                    .id(tripId)
                    .startDate(LocalDate.now())
                    .build();

            Map<UUID, Long> result = tripViewHelper.calculateDaysUntilMap(List.of(trip));

            assertThat(result).containsEntry(tripId, 0L);
        }

        @Test
        @DisplayName("should skip trips with null start date")
        void calculateDaysUntilMap_nullStartDate_shouldSkip() {
            TripResponse trip = TripResponse.builder()
                    .id(tripId)
                    .startDate(null)
                    .build();

            Map<UUID, Long> result = tripViewHelper.calculateDaysUntilMap(List.of(trip));

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("calculateTripDuration")
    class CalculateTripDuration {

        @Test
        @DisplayName("should calculate days and nights correctly")
        void calculateTripDuration_validDates_shouldCalculate() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.of(2026, 3, 1))
                    .endDate(LocalDate.of(2026, 3, 5))
                    .build();

            TripViewHelper.TripDuration result = tripViewHelper.calculateTripDuration(trip);

            assertThat(result.days()).isEqualTo(5L);
            assertThat(result.nights()).isEqualTo(4L);
        }

        @Test
        @DisplayName("should handle single day trip")
        void calculateTripDuration_sameDay_shouldBeOneDayZeroNights() {
            LocalDate date = LocalDate.of(2026, 3, 1);
            TripResponse trip = TripResponse.builder()
                    .startDate(date)
                    .endDate(date)
                    .build();

            TripViewHelper.TripDuration result = tripViewHelper.calculateTripDuration(trip);

            assertThat(result.days()).isEqualTo(1L);
            assertThat(result.nights()).isEqualTo(0L);
        }

        @Test
        @DisplayName("should return zero for null dates")
        void calculateTripDuration_nullDates_shouldReturnZero() {
            TripResponse trip = TripResponse.builder().build();

            TripViewHelper.TripDuration result = tripViewHelper.calculateTripDuration(trip);

            assertThat(result.days()).isEqualTo(0L);
            assertThat(result.nights()).isEqualTo(0L);
            assertThat(result.daysUntil()).isNull();
        }

        @Test
        @DisplayName("should calculate daysUntil for future trip")
        void calculateTripDuration_futureTrip_shouldHaveDaysUntil() {
            LocalDate future = LocalDate.now().plusDays(7);
            TripResponse trip = TripResponse.builder()
                    .startDate(future)
                    .endDate(future.plusDays(3))
                    .build();

            TripViewHelper.TripDuration result = tripViewHelper.calculateTripDuration(trip);

            assertThat(result.daysUntil()).isEqualTo(7L);
        }

        @Test
        @DisplayName("should have null daysUntil for past trip")
        void calculateTripDuration_pastTrip_shouldHaveNullDaysUntil() {
            LocalDate past = LocalDate.now().minusDays(5);
            TripResponse trip = TripResponse.builder()
                    .startDate(past)
                    .endDate(past.plusDays(3))
                    .build();

            TripViewHelper.TripDuration result = tripViewHelper.calculateTripDuration(trip);

            assertThat(result.daysUntil()).isNull();
        }
    }

    @Nested
    @DisplayName("getTodoPreview")
    class GetTodoPreview {

        @Test
        @DisplayName("should return todo preview with counts")
        void getTodoPreview_withTodos_shouldReturnPreview() {
            TodoResponse pending1 = TodoResponse.builder()
                    .id(UUID.randomUUID()).status(TodoStatus.PENDING).build();
            TodoResponse inProgress1 = TodoResponse.builder()
                    .id(UUID.randomUUID()).status(TodoStatus.IN_PROGRESS).build();
            TodoResponse completed1 = TodoResponse.builder()
                    .id(UUID.randomUUID()).status(TodoStatus.COMPLETED).build();

            when(todoService.getTodosByTrip(tripId, userId))
                    .thenReturn(List.of(pending1, inProgress1, completed1));

            TripViewHelper.TodoPreview result = tripViewHelper.getTodoPreview(tripId, userId);

            assertThat(result.totalCount()).isEqualTo(3L);
            assertThat(result.completedCount()).isEqualTo(1L);
            assertThat(result.upcomingTodos()).hasSize(2); // only PENDING and IN_PROGRESS
        }

        @Test
        @DisplayName("should limit upcoming todos to 3")
        void getTodoPreview_manyTodos_shouldLimitToThree() {
            List<TodoResponse> todos = List.of(
                    TodoResponse.builder().id(UUID.randomUUID()).status(TodoStatus.PENDING).build(),
                    TodoResponse.builder().id(UUID.randomUUID()).status(TodoStatus.PENDING).build(),
                    TodoResponse.builder().id(UUID.randomUUID()).status(TodoStatus.PENDING).build(),
                    TodoResponse.builder().id(UUID.randomUUID()).status(TodoStatus.PENDING).build(),
                    TodoResponse.builder().id(UUID.randomUUID()).status(TodoStatus.PENDING).build());

            when(todoService.getTodosByTrip(tripId, userId)).thenReturn(todos);

            TripViewHelper.TodoPreview result = tripViewHelper.getTodoPreview(tripId, userId);

            assertThat(result.upcomingTodos()).hasSize(3);
        }

        @Test
        @DisplayName("should return empty preview on exception")
        void getTodoPreview_onException_shouldReturnEmpty() {
            when(todoService.getTodosByTrip(tripId, userId))
                    .thenThrow(new RuntimeException("Service error"));

            TripViewHelper.TodoPreview result = tripViewHelper.getTodoPreview(tripId, userId);

            assertThat(result.totalCount()).isEqualTo(0L);
            assertThat(result.completedCount()).isEqualTo(0L);
            assertThat(result.upcomingTodos()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getWeatherFallbackCoordinates")
    class GetWeatherFallbackCoordinates {

        @Test
        @DisplayName("should return default Taipei coordinates when no activities")
        void getWeatherFallbackCoordinates_noActivities_shouldReturnDefault() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.now())
                    .build();

            TripViewHelper.WeatherCoordinates result =
                    tripViewHelper.getWeatherFallbackCoordinates(List.of(), trip);

            assertThat(result.latitude()).isEqualTo(25.0339);
            assertThat(result.longitude()).isEqualTo(121.5645);
        }

        @Test
        @DisplayName("should use today's activity coordinates when available")
        void getWeatherFallbackCoordinates_todayActivity_shouldUseTodayCoords() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.now())
                    .build();

            PlaceResponse place = PlaceResponse.builder()
                    .latitude(35.6762)
                    .longitude(139.6503)
                    .build();

            ActivityResponse activity = ActivityResponse.builder()
                    .day(1)
                    .sortOrder(1)
                    .place(place)
                    .build();

            TripViewHelper.WeatherCoordinates result =
                    tripViewHelper.getWeatherFallbackCoordinates(List.of(activity), trip);

            assertThat(result.latitude()).isEqualTo(35.6762);
            assertThat(result.longitude()).isEqualTo(139.6503);
        }

        @Test
        @DisplayName("should fallback to any activity when no today activity")
        void getWeatherFallbackCoordinates_noTodayActivity_shouldUseAny() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.now().minusDays(5))
                    .build();

            PlaceResponse place = PlaceResponse.builder()
                    .latitude(34.6937)
                    .longitude(135.5023)
                    .build();

            // Day 1 activity (5 days ago, not today)
            ActivityResponse activity = ActivityResponse.builder()
                    .day(1)
                    .sortOrder(1)
                    .place(place)
                    .build();

            TripViewHelper.WeatherCoordinates result =
                    tripViewHelper.getWeatherFallbackCoordinates(List.of(activity), trip);

            assertThat(result.latitude()).isEqualTo(34.6937);
            assertThat(result.longitude()).isEqualTo(135.5023);
        }

        @Test
        @DisplayName("should skip activities with zero coordinates")
        void getWeatherFallbackCoordinates_zeroCoords_shouldSkip() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.now())
                    .build();

            PlaceResponse zeroPlace = PlaceResponse.builder()
                    .latitude(0)
                    .longitude(0)
                    .build();

            ActivityResponse activity = ActivityResponse.builder()
                    .day(1)
                    .sortOrder(1)
                    .place(zeroPlace)
                    .build();

            TripViewHelper.WeatherCoordinates result =
                    tripViewHelper.getWeatherFallbackCoordinates(List.of(activity), trip);

            // Should return default since zero coords are skipped
            assertThat(result.latitude()).isEqualTo(25.0339);
            assertThat(result.longitude()).isEqualTo(121.5645);
        }

        @Test
        @DisplayName("should skip activities with null place")
        void getWeatherFallbackCoordinates_nullPlace_shouldSkip() {
            TripResponse trip = TripResponse.builder()
                    .startDate(LocalDate.now())
                    .build();

            ActivityResponse activity = ActivityResponse.builder()
                    .day(1)
                    .sortOrder(1)
                    .place(null)
                    .build();

            TripViewHelper.WeatherCoordinates result =
                    tripViewHelper.getWeatherFallbackCoordinates(List.of(activity), trip);

            assertThat(result.latitude()).isEqualTo(25.0339);
            assertThat(result.longitude()).isEqualTo(121.5645);
        }
    }

    @Nested
    @DisplayName("getExpenseSummary")
    class GetExpenseSummary {

        @Test
        @DisplayName("should return expense summary with correct calculations")
        void getExpenseSummary_withExpenses_shouldCalculate() {
            when(expenseService.getExpenseCount(tripId, userId)).thenReturn(10L);
            when(documentService.getDocumentCount(tripId, userId)).thenReturn(5L);
            when(expenseService.getTotalExpense(tripId, "TWD", userId))
                    .thenReturn(new BigDecimal("10000"));

            TripViewHelper.ExpenseSummary result =
                    tripViewHelper.getExpenseSummary(tripId, userId, "TWD", 4);

            assertThat(result.expenseCount()).isEqualTo(10L);
            assertThat(result.documentCount()).isEqualTo(5L);
            assertThat(result.totalExpense()).isEqualByComparingTo("10000");
            assertThat(result.averageExpense()).isEqualByComparingTo("2500");
        }

        @Test
        @DisplayName("should return zero average when no expenses")
        void getExpenseSummary_noExpenses_shouldReturnZeroAverage() {
            when(expenseService.getExpenseCount(tripId, userId)).thenReturn(0L);
            when(documentService.getDocumentCount(tripId, userId)).thenReturn(0L);
            when(expenseService.getTotalExpense(tripId, "TWD", userId))
                    .thenReturn(BigDecimal.ZERO);

            TripViewHelper.ExpenseSummary result =
                    tripViewHelper.getExpenseSummary(tripId, userId, "TWD", 3);

            assertThat(result.averageExpense()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("should handle zero member count by defaulting divisor to 1")
        void getExpenseSummary_zeroMembers_shouldDefaultDivisorToOne() {
            when(expenseService.getExpenseCount(tripId, userId)).thenReturn(1L);
            when(documentService.getDocumentCount(tripId, userId)).thenReturn(0L);
            when(expenseService.getTotalExpense(tripId, "TWD", userId))
                    .thenReturn(new BigDecimal("5000"));

            TripViewHelper.ExpenseSummary result =
                    tripViewHelper.getExpenseSummary(tripId, userId, "TWD", 0);

            assertThat(result.averageExpense()).isEqualByComparingTo("5000");
        }
    }
}
