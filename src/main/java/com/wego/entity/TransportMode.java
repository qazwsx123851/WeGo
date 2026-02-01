package com.wego.entity;

/**
 * Enum representing transportation modes between activities.
 *
 * @contract
 *   - Basic modes (WALKING, TRANSIT, DRIVING, BICYCLING) support Google Maps API
 *   - Extended modes (FLIGHT, HIGH_SPEED_RAIL) require manual duration input
 *   - NOT_CALCULATED skips transport calculation entirely
 *   - calledBy: TransportCalculationService, ActivityService
 *
 * @see Activity
 */
public enum TransportMode {

    /**
     * Walking
     */
    WALKING,

    /**
     * Public transit (bus, train, subway)
     */
    TRANSIT,

    /**
     * Driving (car, taxi, rideshare)
     */
    DRIVING,

    /**
     * Cycling
     */
    BICYCLING,

    /**
     * Air travel - requires manual duration input.
     * Google Maps API does not support flight routing.
     */
    FLIGHT,

    /**
     * High-speed rail (e.g., Taiwan HSR, Japan Shinkansen).
     * May require manual duration input for accuracy.
     */
    HIGH_SPEED_RAIL,

    /**
     * Transport calculation intentionally skipped.
     * Used when user doesn't want to track transport between activities.
     */
    NOT_CALCULATED;

    /**
     * Returns the Google Maps API mode string.
     *
     * @return Google Maps mode parameter value, or null for unsupported modes
     */
    public String toGoogleMapsMode() {
        return switch (this) {
            case WALKING, TRANSIT, DRIVING, BICYCLING -> name().toLowerCase();
            case FLIGHT, HIGH_SPEED_RAIL, NOT_CALCULATED -> null;
        };
    }

    /**
     * Checks if this mode supports automatic calculation via Google Maps API.
     *
     * @return true if Google Maps can calculate route for this mode
     */
    public boolean supportsAutoCalculation() {
        return switch (this) {
            case WALKING, TRANSIT, DRIVING, BICYCLING -> true;
            case FLIGHT, HIGH_SPEED_RAIL, NOT_CALCULATED -> false;
        };
    }

    /**
     * Checks if this mode requires manual duration input from user.
     *
     * @return true if user must provide duration manually
     */
    public boolean requiresManualInput() {
        return switch (this) {
            case FLIGHT, HIGH_SPEED_RAIL -> true;
            default -> false;
        };
    }

    /**
     * Returns user-friendly display name in Traditional Chinese.
     *
     * @return Display name for UI
     */
    public String getDisplayName() {
        return switch (this) {
            case WALKING -> "步行";
            case TRANSIT -> "大眾運輸";
            case DRIVING -> "開車";
            case BICYCLING -> "騎車";
            case FLIGHT -> "飛機";
            case HIGH_SPEED_RAIL -> "高鐵";
            case NOT_CALCULATED -> "不計算";
        };
    }
}
