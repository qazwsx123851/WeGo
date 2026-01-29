package com.wego.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Place entity representing a geographic location.
 *
 * Places can be created from Google Places API or manually by users.
 * Multiple activities can reference the same place.
 *
 * @contract
 *   - invariant: name is never null or empty
 *   - invariant: latitude is between -90 and 90
 *   - invariant: longitude is between -180 and 180
 *
 * @see Activity
 */
@Entity
@Table(name = "places")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Place {

    private static final double EARTH_RADIUS_METERS = 6_371_000;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "google_place_id", length = 100, unique = true)
    private String googlePlaceId;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "website", length = 500)
    private String website;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "price_level")
    private Integer priceLevel;

    @Column(name = "photo_reference", length = 500)
    private String photoReference;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Calculates the distance to another place using Haversine formula.
     *
     * @contract
     *   - pre: other != null
     *   - post: returns distance in meters >= 0
     *   - calledBy: RouteOptimizer, ActivityService
     *
     * @param other The other place
     * @return Distance in meters
     */
    public double distanceTo(Place other) {
        double lat1 = Math.toRadians(this.latitude);
        double lat2 = Math.toRadians(other.latitude);
        double deltaLat = Math.toRadians(other.latitude - this.latitude);
        double deltaLon = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Equality based on ID for JPA entity identity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Place place = (Place) o;
        return id != null && Objects.equals(id, place.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Place{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
