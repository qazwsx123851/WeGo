package com.wego.domain.geo;

/**
 * Utility class for geographic distance calculations.
 * Consolidates the Haversine formula previously duplicated across
 * RouteOptimizer, Place, TransportCalculationService, and MockGoogleMapsClient.
 */
public final class GeoUtils {

    public static final double EARTH_RADIUS_METERS = 6_371_000;

    private GeoUtils() {
    }

    /**
     * Calculates the great-circle distance between two points using the Haversine formula.
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lng1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lng2 Longitude of point 2 in degrees
     * @return Distance in meters
     */
    public static double haversineDistanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }
}
