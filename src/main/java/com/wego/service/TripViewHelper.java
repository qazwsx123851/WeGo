package com.wego.service;

import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.TodoResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.TodoStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * View helper for Trip-related view logic.
 * Extracts computational/presentation logic from TripController and HomeController.
 *
 * @contract
 *   - pre: Services are injected
 *   - post: Provides view-preparation methods
 *   - calledBy: TripController, HomeController
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripViewHelper {

    private final ActivityService activityService;
    private final TodoService todoService;
    private final ExpenseService expenseService;
    private final DocumentService documentService;

    /**
     * Calculates days-until-departure map for a list of trips.
     * Shared by HomeController dashboard and TripController list.
     *
     * @param trips List of trip responses
     * @return Map of trip ID to days until departure (only future trips included)
     */
    public Map<UUID, Long> calculateDaysUntilMap(List<TripResponse> trips) {
        LocalDate today = LocalDate.now();
        return trips.stream()
                .filter(trip -> trip.getStartDate() != null && !trip.getStartDate().isBefore(today))
                .collect(Collectors.toMap(
                        TripResponse::getId,
                        trip -> ChronoUnit.DAYS.between(today, trip.getStartDate())
                ));
    }

    /**
     * Holds trip duration info for the view.
     */
    public record TripDuration(long days, long nights, Long daysUntil) {}

    /**
     * Calculates trip duration and days until departure.
     *
     * @param trip The trip response
     * @return TripDuration with days, nights, and optional daysUntil
     */
    public TripDuration calculateTripDuration(TripResponse trip) {
        long tripDays = 0L;
        long tripNights = 0L;
        Long daysUntil = null;

        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            tripDays = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
            tripNights = tripDays - 1;
        }

        if (trip.getStartDate() != null && trip.getStartDate().isAfter(LocalDate.now())) {
            daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), trip.getStartDate());
        }

        return new TripDuration(tripDays, tripNights, daysUntil);
    }

    /**
     * Holds todo preview data for the trip detail view.
     */
    public record TodoPreview(long totalCount, long completedCount, List<TodoResponse> upcomingTodos) {}

    /**
     * Loads todo preview data for the trip detail page.
     *
     * @param tripId The trip ID
     * @param userId The user ID for permission check
     * @return TodoPreview with counts and upcoming items
     */
    public TodoPreview getTodoPreview(UUID tripId, UUID userId) {
        try {
            List<TodoResponse> allTodos = todoService.getTodosByTrip(tripId, userId);
            long totalTodos = allTodos.size();
            long completedTodos = allTodos.stream()
                    .filter(t -> t.getStatus() == TodoStatus.COMPLETED)
                    .count();

            List<TodoResponse> upcomingTodos = allTodos.stream()
                    .filter(t -> t.getStatus() == TodoStatus.PENDING || t.getStatus() == TodoStatus.IN_PROGRESS)
                    .limit(3)
                    .collect(Collectors.toList());

            log.debug("Todo data loaded: total={}, completed={}, upcoming={}",
                    totalTodos, completedTodos, upcomingTodos.size());
            return new TodoPreview(totalTodos, completedTodos, upcomingTodos);
        } catch (Exception e) {
            log.warn("Failed to load todo data for trip {}: {}", tripId, e.getMessage());
            return new TodoPreview(0L, 0L, List.of());
        }
    }

    /**
     * Holds weather fallback coordinate data.
     */
    public record WeatherCoordinates(double latitude, double longitude) {}

    /**
     * Determines weather fallback coordinates from trip activities.
     *
     * Priority:
     * 1. Today's first activity with coordinates
     * 2. Any activity with coordinates (sorted by day)
     * 3. Default: Taipei 101 (25.0339, 121.5645)
     *
     * @param activities List of activities for the trip
     * @param trip The trip response (for start date reference)
     * @return WeatherCoordinates with lat/lng
     */
    public WeatherCoordinates getWeatherFallbackCoordinates(
            List<ActivityResponse> activities, TripResponse trip) {

        final double defaultLat = 25.0339;
        final double defaultLng = 121.5645;

        LocalDate today = LocalDate.now();
        LocalDate tripStart = trip.getStartDate();
        int todayDayNumber = (int) ChronoUnit.DAYS.between(tripStart, today) + 1;

        // Priority 1: Today's first activity with coordinates
        var todayActivity = activities.stream()
                .filter(a -> a.getDay() == todayDayNumber)
                .filter(a -> a.getPlace() != null)
                .filter(a -> a.getPlace().getLatitude() != 0 || a.getPlace().getLongitude() != 0)
                .min((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));

        if (todayActivity.isPresent()) {
            var place = todayActivity.get().getPlace();
            log.debug("Weather fallback: today's activity at ({}, {})", place.getLatitude(), place.getLongitude());
            return new WeatherCoordinates(place.getLatitude(), place.getLongitude());
        }

        // Priority 2: Any activity with coordinates
        var anyActivity = activities.stream()
                .filter(a -> a.getPlace() != null)
                .filter(a -> a.getPlace().getLatitude() != 0 || a.getPlace().getLongitude() != 0)
                .min((a, b) -> {
                    int dayCompare = Integer.compare(a.getDay(), b.getDay());
                    return dayCompare != 0 ? dayCompare : Integer.compare(a.getSortOrder(), b.getSortOrder());
                });

        if (anyActivity.isPresent()) {
            var place = anyActivity.get().getPlace();
            log.debug("Weather fallback: first activity at ({}, {})", place.getLatitude(), place.getLongitude());
            return new WeatherCoordinates(place.getLatitude(), place.getLongitude());
        }

        // Priority 3: Default to Taipei 101
        log.debug("Weather fallback: default Taipei 101 ({}, {})", defaultLat, defaultLng);
        return new WeatherCoordinates(defaultLat, defaultLng);
    }

    /**
     * Holds expense summary data for the trip detail view.
     */
    public record ExpenseSummary(long expenseCount, long documentCount,
                                  BigDecimal totalExpense, BigDecimal averageExpense) {}

    /**
     * Loads expense and document summary for the trip detail page.
     *
     * @param tripId The trip ID
     * @param userId The user ID for permission check
     * @param baseCurrency The trip's base currency
     * @param memberCount The number of members in the trip
     * @return ExpenseSummary with counts and totals
     */
    public ExpenseSummary getExpenseSummary(UUID tripId, UUID userId, String baseCurrency, int memberCount) {
        long expenseCount = expenseService.getExpenseCount(tripId, userId);
        long documentCount = documentService.getDocumentCount(tripId, userId);

        BigDecimal totalExpense = expenseService.getTotalExpense(tripId, baseCurrency, userId);

        int divisor = memberCount > 0 ? memberCount : 1;
        BigDecimal averageExpense = totalExpense.compareTo(BigDecimal.ZERO) > 0
                ? totalExpense.divide(BigDecimal.valueOf(divisor), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new ExpenseSummary(expenseCount, documentCount, totalExpense, averageExpense);
    }
}
