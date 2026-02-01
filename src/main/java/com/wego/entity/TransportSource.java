package com.wego.entity;

/**
 * Enum representing the source of transport calculation data.
 *
 * Used to indicate how the transport duration/distance was determined,
 * allowing the UI to show appropriate warnings for estimated values.
 *
 * @contract
 *   - GOOGLE_API: Data from Google Maps Directions API (most accurate)
 *   - HAVERSINE: Fallback straight-line distance calculation (estimated)
 *   - MANUAL: User-provided values (e.g., for flights)
 *   - NOT_APPLICABLE: Transport calculation skipped
 *   - calledBy: TransportCalculationService, ActivityService
 *
 * @see Activity
 * @see TransportWarning
 */
public enum TransportSource {

    /**
     * Data retrieved from Google Maps Directions API.
     * This is the most accurate source as it considers actual routes.
     */
    GOOGLE_API,

    /**
     * Fallback calculation using Haversine formula.
     * This provides straight-line distance and estimated duration
     * based on assumed average speeds. Used when Google API fails
     * or returns no route (e.g., cross-water routes).
     */
    HAVERSINE,

    /**
     * User manually entered the transport duration.
     * Typically used for flights, ferries, or special transport.
     */
    MANUAL,

    /**
     * Transport calculation was intentionally skipped.
     * Used when transportMode is NOT_CALCULATED or for
     * the first activity of each day.
     */
    NOT_APPLICABLE;

    /**
     * Gets a user-friendly display name in Traditional Chinese.
     *
     * @return Display name for UI
     */
    public String getDisplayName() {
        return switch (this) {
            case GOOGLE_API -> "精確";
            case HAVERSINE -> "估算";
            case MANUAL -> "手動";
            case NOT_APPLICABLE -> "N/A";
        };
    }

    /**
     * Gets the Tailwind CSS badge classes for styling.
     *
     * @return CSS classes for badge styling
     */
    public String getBadgeClass() {
        return switch (this) {
            case GOOGLE_API -> "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400";
            case HAVERSINE -> "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400";
            case MANUAL -> "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400";
            case NOT_APPLICABLE -> "bg-gray-100 text-gray-500 dark:bg-gray-700 dark:text-gray-400";
        };
    }

    /**
     * Gets a tooltip description explaining the source.
     *
     * @return Tooltip text for user guidance
     */
    public String getTooltip() {
        return switch (this) {
            case GOOGLE_API -> "使用 Google Maps 計算實際路線";
            case HAVERSINE -> "直線距離估算，實際路線可能較長";
            case MANUAL -> "手動輸入的交通時間";
            case NOT_APPLICABLE -> "無需計算（首個景點）";
        };
    }
}
