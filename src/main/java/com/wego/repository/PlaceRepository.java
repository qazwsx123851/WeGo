package com.wego.repository;

import com.wego.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Place entity operations.
 *
 * @contract
 *   - pre: All parameters must be non-null unless marked Optional
 *   - post: Returns Optional.empty() when no match found
 *   - calledBy: ActivityService, GoogleMapsService
 */
@Repository
public interface PlaceRepository extends JpaRepository<Place, UUID> {

    /**
     * Finds a place by its Google Place ID.
     *
     * @contract
     *   - pre: googlePlaceId != null
     *   - post: Returns Optional containing place if found
     *
     * @param googlePlaceId The Google Place ID
     * @return Optional containing the place if found
     */
    Optional<Place> findByGooglePlaceId(String googlePlaceId);

    /**
     * Checks if a place exists with the given Google Place ID.
     *
     * @param googlePlaceId The Google Place ID
     * @return true if place exists
     */
    boolean existsByGooglePlaceId(String googlePlaceId);
}
