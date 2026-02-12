package com.wego.domain;

/**
 * Shared constants for trip-related domain logic.
 *
 * @contract
 *   - All constants are immutable
 *   - Used by TripService and InviteLinkService
 */
public final class TripConstants {

    public static final int MAX_MEMBERS_PER_TRIP = 10;

    private TripConstants() {
        // Utility class
    }
}
