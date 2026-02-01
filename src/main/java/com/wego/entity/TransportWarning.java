package com.wego.entity;

/**
 * Enum representing warnings related to transport calculations.
 *
 * Used to alert users when calculated transport data may be
 * inaccurate or unrealistic, allowing them to make informed decisions.
 *
 * @contract
 *   - Warnings are determined by TransportCalculationService
 *   - Multiple conditions can trigger warnings based on distance and mode
 *   - UI should display appropriate messages based on warning type
 *   - calledBy: TransportCalculationService
 *
 * @see Activity
 * @see TransportSource
 */
public enum TransportWarning {

    /**
     * No warning - transport data is considered reliable.
     */
    NONE,

    /**
     * Distance was calculated using Haversine formula (straight-line).
     * Actual route may be significantly different.
     * Triggered when TransportSource is HAVERSINE.
     */
    ESTIMATED_DISTANCE,

    /**
     * Walking distance exceeds reasonable threshold (> 5 km).
     * Suggests user should consider alternative transport mode.
     */
    UNREALISTIC_WALKING,

    /**
     * Bicycling distance exceeds reasonable threshold (> 30 km).
     * Suggests user should consider alternative transport mode.
     */
    UNREALISTIC_BICYCLING,

    /**
     * Distance exceeds 100 km for any transport mode.
     * May indicate cross-country travel requiring special transport.
     */
    VERY_LONG_DISTANCE,

    /**
     * No route available from Google Maps API.
     * Typically indicates cross-water routes or impassable terrain.
     */
    NO_ROUTE_AVAILABLE;

    /**
     * Returns a user-friendly warning message in Traditional Chinese.
     *
     * @return Warning message suitable for UI display
     */
    public String getDisplayMessage() {
        return switch (this) {
            case NONE -> null;
            case ESTIMATED_DISTANCE -> "此為直線距離估算，實際路線可能不同";
            case UNREALISTIC_WALKING -> "步行距離較長，建議改用其他交通方式";
            case UNREALISTIC_BICYCLING -> "騎車距離較長，建議改用其他交通方式";
            case VERY_LONG_DISTANCE -> "距離超過 100 公里，請確認交通方式是否正確";
            case NO_ROUTE_AVAILABLE -> "無法計算實際路線，顯示為直線距離估算";
        };
    }

    /**
     * Returns the severity level for UI styling.
     *
     * @return "info" for ESTIMATED_DISTANCE, "warning" for others
     */
    public String getSeverity() {
        return switch (this) {
            case NONE -> null;
            case ESTIMATED_DISTANCE -> "info";
            default -> "warning";
        };
    }
}
