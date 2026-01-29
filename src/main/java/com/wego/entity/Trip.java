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
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Trip entity representing a travel itinerary.
 *
 * A trip has an owner who creates it and can invite other members
 * to collaborate. Each trip has a date range and can contain
 * activities, expenses, documents, and todos.
 *
 * @contract
 *   - invariant: title is never null or empty (max 100 chars)
 *   - invariant: startDate is never null
 *   - invariant: endDate is never null and >= startDate
 *   - invariant: baseCurrency is always a valid 3-letter currency code
 *   - invariant: ownerId is never null
 *
 * @see TripMember for membership relationship
 * @see Activity for trip activities
 * @see Expense for trip expenses
 */
@Entity
@Table(name = "trips")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "base_currency", nullable = false, length = 3)
    @Builder.Default
    private String baseCurrency = "TWD";

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Calculates the duration of the trip in days.
     *
     * @contract
     *   - pre: startDate != null && endDate != null
     *   - post: returns value >= 1
     *   - calledBy: TripService, UI templates
     *
     * @return the number of days including both start and end dates
     */
    public int getDurationDays() {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * Equality based on ID for JPA entity identity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Trip trip = (Trip) o;
        return id != null && Objects.equals(id, trip.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Trip{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", ownerId=" + ownerId +
                '}';
    }
}
