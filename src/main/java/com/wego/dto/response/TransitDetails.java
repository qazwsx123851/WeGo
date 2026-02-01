package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Transit-specific details from Routes API response.
 *
 * Contains information about public transportation lines, stops,
 * and fare when available.
 *
 * @contract
 *   - Only populated when travel mode is TRANSIT
 *   - All fields are optional (may be null)
 *   - calledBy: DirectionResult
 *
 * @see DirectionResult
 * @see <a href="https://developers.google.com/maps/documentation/routes/transit-route">Routes API Transit</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitDetails {

    /**
     * Type of transit vehicle.
     */
    public enum VehicleType {
        BUS,
        CABLE_CAR,
        COMMUTER_TRAIN,
        FERRY,
        FUNICULAR,
        GONDOLA_LIFT,
        HEAVY_RAIL,
        HIGH_SPEED_TRAIN,
        INTERCITY_BUS,
        LONG_DISTANCE_TRAIN,
        METRO_RAIL,
        MONORAIL,
        OTHER,
        RAIL,
        SHARE_TAXI,
        SUBWAY,
        TRAM,
        TROLLEYBUS
    }

    /**
     * Name of the transit line (e.g., "JR Haruka", "Osaka Metro Midosuji Line").
     */
    private String lineName;

    /**
     * Short name of the line (e.g., "M", "JR-A").
     */
    private String lineShortName;

    /**
     * Type of vehicle used on this line.
     */
    private VehicleType vehicleType;

    /**
     * Name of the transit agency operating this line.
     */
    private String agencyName;

    /**
     * Headsign/destination displayed on the vehicle.
     */
    private String headsign;

    /**
     * Name of the departure stop/station.
     */
    private String departureStop;

    /**
     * Name of the arrival stop/station.
     */
    private String arrivalStop;

    /**
     * Number of stops on this segment.
     */
    private Integer numberOfStops;

    /**
     * Line color in hex format (e.g., "#FF0000").
     */
    private String lineColor;

    /**
     * Text color for displaying on the line color (e.g., "#FFFFFF").
     */
    private String lineTextColor;

    /**
     * Estimated fare amount (in local currency).
     */
    private Double fareAmount;

    /**
     * Currency code for fare (e.g., "JPY", "TWD").
     */
    private String fareCurrency;
}
