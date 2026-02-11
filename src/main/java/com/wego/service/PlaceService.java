package com.wego.service;

import com.wego.entity.Place;
import com.wego.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for Place-related business logic.
 *
 * @contract
 *   - Handles find-or-create logic for Place entities
 *   - calledBy: ActivityWebController
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlaceService {

    private final PlaceRepository placeRepository;

    /**
     * Finds an existing place by Google Place ID, or creates a new one.
     *
     * @contract
     *   - pre: name != null
     *   - post: returns a persisted Place entity
     *   - calls: PlaceRepository#findByGooglePlaceId, PlaceRepository#save
     *
     * @param googlePlaceId Google Place ID (nullable)
     * @param name Place name
     * @param address Place address
     * @param latitude Latitude (nullable, defaults to 0.0)
     * @param longitude Longitude (nullable, defaults to 0.0)
     * @param type Activity type for category mapping
     * @return Persisted Place entity
     */
    @Transactional
    public Place findOrCreate(String googlePlaceId, String name, String address,
                              Double latitude, Double longitude, String type) {
        Place place = null;

        if (googlePlaceId != null && !googlePlaceId.isEmpty()) {
            place = placeRepository.findByGooglePlaceId(googlePlaceId).orElse(null);
        }

        if (place == null) {
            place = Place.builder()
                    .name(name)
                    .address(address)
                    .latitude(latitude != null ? latitude : 0.0)
                    .longitude(longitude != null ? longitude : 0.0)
                    .googlePlaceId(googlePlaceId)
                    .category(mapTypeToCategory(type))
                    .build();
            place = placeRepository.save(place);
            log.info("Created new place: {} with id {} category {}", name, place.getId(), place.getCategory());
        } else {
            place.setCategory(mapTypeToCategory(type));
            place = placeRepository.save(place);
        }

        return place;
    }

    /**
     * Maps activity form type to Place category.
     *
     * @param type The form value (ATTRACTION, RESTAURANT, TRANSPORT, ACCOMMODATION)
     * @return The Place category value, or null for ATTRACTION
     */
    public static String mapTypeToCategory(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "RESTAURANT" -> "restaurant";
            case "TRANSPORT" -> "transit_station";
            case "ACCOMMODATION" -> "lodging";
            default -> null;
        };
    }
}
