package com.wego.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Transit preferences for Routes API requests.
 *
 * Allows specifying preferred transit modes and routing preferences
 * when calculating public transportation routes.
 *
 * @contract
 *   - allowedTravelModes: List of allowed transit types (BUS, SUBWAY, TRAIN, etc.)
 *   - routingPreference: LESS_WALKING or FEWER_TRANSFERS
 *   - calledBy: GoogleMapsClient implementations
 *
 * @see <a href="https://developers.google.com/maps/documentation/routes/transit-route">Routes API Transit</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitPreferences {

    /**
     * Allowed transit travel modes.
     */
    public enum TransitTravelMode {
        BUS,
        SUBWAY,
        TRAIN,
        LIGHT_RAIL,
        RAIL
    }

    /**
     * Transit routing preference.
     */
    public enum TransitRoutingPreference {
        /** Prefer routes with less walking */
        LESS_WALKING,
        /** Prefer routes with fewer transfers */
        FEWER_TRANSFERS
    }

    /**
     * List of allowed transit travel modes.
     * If empty or null, all modes are allowed.
     */
    private List<TransitTravelMode> allowedTravelModes;

    /**
     * Routing preference for transit routes.
     */
    private TransitRoutingPreference routingPreference;

    /**
     * Creates default transit preferences (all modes, less walking).
     */
    public static TransitPreferences defaults() {
        return TransitPreferences.builder()
                .allowedTravelModes(List.of(
                        TransitTravelMode.BUS,
                        TransitTravelMode.SUBWAY,
                        TransitTravelMode.TRAIN,
                        TransitTravelMode.RAIL
                ))
                .routingPreference(TransitRoutingPreference.LESS_WALKING)
                .build();
    }
}
