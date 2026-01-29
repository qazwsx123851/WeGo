package com.wego.entity;

/**
 * Enum representing transportation modes between activities.
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
    BICYCLING;

    /**
     * Returns the Google Maps API mode string.
     *
     * @return Google Maps mode parameter value
     */
    public String toGoogleMapsMode() {
        return name().toLowerCase();
    }
}
