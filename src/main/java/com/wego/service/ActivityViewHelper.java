package com.wego.service;

import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.TripResponse;
import com.wego.entity.TransportMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class for activity view logic extracted from ActivityWebController.
 *
 * Contains pure business logic for grouping, date generation, and transport validation.
 * No Spring web dependencies (HttpServletRequest, Model, etc.).
 *
 * @contract
 *   - All methods are pure functions with no side effects
 *   - calledBy: ActivityWebController
 */
@Component
@Slf4j
public class ActivityViewHelper {

    /**
     * Groups activities by their scheduled date based on the trip's date range.
     *
     * Initializes all dates from trip start to end with empty lists,
     * then assigns each activity to its corresponding date.
     * Activities within each day are sorted by sortOrder.
     *
     * @contract
     *   - pre: trip != null, activities != null
     *   - pre: trip.startDate and trip.endDate must not be null
     *   - post: returned map has an entry for every date in the trip range
     *   - post: activities within each date are sorted by sortOrder ascending
     *   - post: activities with invalid day values are excluded
     *
     * @param trip       the trip containing start/end dates
     * @param activities the list of activities to group
     * @return ordered map of dates to activity lists
     */
    public Map<LocalDate, List<ActivityResponse>> groupActivitiesByDate(
            TripResponse trip, List<ActivityResponse> activities) {

        Map<LocalDate, List<ActivityResponse>> activitiesByDate = new LinkedHashMap<>();

        List<LocalDate> dates = generateTripDates(trip);

        // Initialize all dates with empty lists
        for (LocalDate date : dates) {
            activitiesByDate.put(date, new ArrayList<>());
        }

        // Group activities by their scheduled date
        for (ActivityResponse activity : activities) {
            int activityDay = activity.getDay();
            if (activityDay >= 1 && activityDay <= dates.size()) {
                LocalDate activityDate = trip.getStartDate().plusDays(activityDay - 1);
                activitiesByDate.computeIfAbsent(activityDate, k -> new ArrayList<>())
                        .add(activity);
            }
        }

        // Sort activities within each day by sortOrder
        for (List<ActivityResponse> dayActivities : activitiesByDate.values()) {
            dayActivities.sort((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()));
        }

        return activitiesByDate;
    }

    /**
     * Generates a list of dates from trip start date to end date (inclusive).
     *
     * Falls back to a single-element list containing today's date
     * when either start or end date is null.
     *
     * @contract
     *   - pre: trip != null
     *   - post: returned list is never empty
     *   - post: dates are in chronological order
     *   - post: if startDate or endDate is null, returns List.of(LocalDate.now())
     *
     * @param trip the trip containing start/end dates
     * @return list of dates in the trip range
     */
    public List<LocalDate> generateTripDates(TripResponse trip) {
        if (trip.getStartDate() != null && trip.getEndDate() != null) {
            return trip.getStartDate()
                    .datesUntil(trip.getEndDate().plusDays(1))
                    .collect(Collectors.toList());
        }
        return List.of(LocalDate.now());
    }

    /**
     * Validates transport mode and manual transport minutes input.
     *
     * Parses the transport mode string (defaults to WALKING on invalid input),
     * validates manual minutes range (0-2880), and checks whether manual input
     * is required for modes like FLIGHT and HIGH_SPEED_RAIL.
     *
     * @contract
     *   - pre: transportMode != null
     *   - post: result.transportMode is never null
     *   - post: result.manualMinutes is null, or in range [0, 2880]
     *   - post: result.errorMessage is non-null only when validation fails
     *
     * @param transportMode           the transport mode string from form input
     * @param manualTransportMinutes  optional manual duration in minutes
     * @return validation result containing parsed mode, validated minutes, and optional error
     */
    public TransportValidationResult validateTransportInput(
            String transportMode, Integer manualTransportMinutes) {

        // Parse transport mode with WALKING default
        TransportMode parsedTransportMode;
        try {
            parsedTransportMode = TransportMode.valueOf(transportMode);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid transport mode '{}', defaulting to WALKING", transportMode);
            parsedTransportMode = TransportMode.WALKING;
        }

        // Validate manualTransportMinutes range (0-2880, max 48 hours)
        Integer validatedManualMinutes = manualTransportMinutes;
        if (validatedManualMinutes != null) {
            if (validatedManualMinutes < 0) {
                log.warn("Invalid manualTransportMinutes '{}', must be >= 0, setting to null",
                        validatedManualMinutes);
                validatedManualMinutes = null;
            } else if (validatedManualMinutes > 2880) {
                log.warn("manualTransportMinutes '{}' exceeds max (2880), capping",
                        validatedManualMinutes);
                validatedManualMinutes = 2880;
            }
        }

        // Validate: FLIGHT/HIGH_SPEED_RAIL requires manual input
        if (parsedTransportMode.requiresManualInput() &&
            (validatedManualMinutes == null || validatedManualMinutes <= 0)) {
            log.warn("Transport mode '{}' requires manual duration but none provided",
                    parsedTransportMode);
            return new TransportValidationResult(
                    parsedTransportMode,
                    validatedManualMinutes,
                    "選擇飛機或高鐵時，必須輸入預估交通時間"
            );
        }

        return new TransportValidationResult(parsedTransportMode, validatedManualMinutes, null);
    }

    /**
     * Result of transport input validation.
     *
     * @param transportMode  the parsed transport mode (never null)
     * @param manualMinutes  the validated manual minutes (null or in range [0, 2880])
     * @param errorMessage   error message if validation failed, null if valid
     */
    public record TransportValidationResult(
            TransportMode transportMode,
            Integer manualMinutes,
            String errorMessage
    ) {
        /**
         * @return true if validation passed (no error)
         */
        public boolean isValid() {
            return errorMessage == null;
        }
    }
}
