package com.wego.dto.response;

import com.wego.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO for place information.
 *
 * @contract
 *   - id: always present
 *   - name: always present
 *   - latitude/longitude: always present
 *   - other fields may be null
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceResponse {

    private UUID id;
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private String category;
    private Double rating;
    private Integer priceLevel;
    private String photoReference;

    /**
     * Creates a PlaceResponse from a Place entity.
     *
     * @contract
     *   - pre: place != null
     *   - post: returns PlaceResponse with all fields mapped
     *
     * @param place The place entity
     * @return PlaceResponse DTO
     */
    public static PlaceResponse fromEntity(Place place) {
        if (place == null) {
            return null;
        }
        return PlaceResponse.builder()
                .id(place.getId())
                .name(place.getName())
                .address(place.getAddress())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .category(place.getCategory())
                .rating(place.getRating())
                .priceLevel(place.getPriceLevel())
                .photoReference(place.getPhotoReference())
                .build();
    }
}
