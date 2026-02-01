package com.wego.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Activity entity representing a scheduled activity/visit within a trip.
 *
 * Activities are linked to a specific day of the trip and ordered by sortOrder.
 * Each activity references a Place for location information.
 *
 * @contract
 *   - invariant: tripId is never null
 *   - invariant: day >= 1 and <= trip duration
 *   - invariant: sortOrder >= 0
 *   - invariant: durationMinutes >= 0 if set
 *
 * @see Trip
 * @see Place
 */
@Entity
@Table(name = "activities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "place_id")
    private UUID placeId;

    @Column(name = "day", nullable = false)
    private int day;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "note", length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", length = 20)
    @Builder.Default
    private TransportMode transportMode = TransportMode.WALKING;

    @Column(name = "transport_duration_minutes")
    private Integer transportDurationMinutes;

    @Column(name = "transport_distance_meters")
    private Integer transportDistanceMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_source", length = 20)
    @Builder.Default
    private TransportSource transportSource = TransportSource.NOT_APPLICABLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_warning", length = 30)
    @Builder.Default
    private TransportWarning transportWarning = TransportWarning.NONE;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Calculates the end time based on start time and duration.
     *
     * @contract
     *   - pre: startTime != null && durationMinutes != null
     *   - post: returns LocalTime after startTime by durationMinutes
     *   - calledBy: UI templates, ActivityService
     *
     * @return The calculated end time, or null if inputs are missing
     */
    public LocalTime getEndTime() {
        if (startTime == null || durationMinutes == null) {
            return null;
        }
        return startTime.plusMinutes(durationMinutes);
    }

    /**
     * Equality based on ID for JPA entity identity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Activity activity = (Activity) o;
        return id != null && Objects.equals(id, activity.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Activity{" +
                "id=" + id +
                ", tripId=" + tripId +
                ", day=" + day +
                ", sortOrder=" + sortOrder +
                ", startTime=" + startTime +
                '}';
    }
}
