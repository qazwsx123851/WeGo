package com.wego.service;

import com.wego.entity.Place;
import com.wego.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for PlaceService.
 *
 * @contract
 *   - Tests findOrCreate logic for Place entities
 *   - Tests mapTypeToCategory static method
 *   - Verifies find-existing vs create-new behavior
 */
@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    private PlaceRepository placeRepository;

    @InjectMocks
    private PlaceService placeService;

    private UUID placeId;

    @BeforeEach
    void setUp() {
        placeId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("findOrCreate")
    class FindOrCreateTests {

        @Test
        @DisplayName("should create new place when googlePlaceId is null")
        void findOrCreate_nullGooglePlaceId_shouldCreateNew() {
            Place savedPlace = Place.builder()
                    .id(placeId)
                    .name("Test Place")
                    .address("123 Main St")
                    .latitude(25.0)
                    .longitude(121.5)
                    .category(null)
                    .build();
            when(placeRepository.save(any(Place.class))).thenReturn(savedPlace);

            UUID result = placeService.findOrCreate(null, "Test Place", "123 Main St",
                    25.0, 121.5, "ATTRACTION");

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(placeId);
            verify(placeRepository, never()).findByGooglePlaceId(any());
            verify(placeRepository).save(any(Place.class));
        }

        @Test
        @DisplayName("should create new place when googlePlaceId is empty")
        void findOrCreate_emptyGooglePlaceId_shouldCreateNew() {
            Place savedPlace = Place.builder()
                    .id(placeId)
                    .name("Test Place")
                    .build();
            when(placeRepository.save(any(Place.class))).thenReturn(savedPlace);

            UUID result = placeService.findOrCreate("", "Test Place", "123 Main St",
                    25.0, 121.5, "RESTAURANT");

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(placeId);
            verify(placeRepository, never()).findByGooglePlaceId(any());
            verify(placeRepository).save(any(Place.class));
        }

        @Test
        @DisplayName("should find existing place by googlePlaceId and update category")
        void findOrCreate_existingGooglePlaceId_shouldFindAndUpdateCategory() {
            String googlePlaceId = "ChIJ123456";
            Place existingPlace = Place.builder()
                    .id(placeId)
                    .name("Existing Place")
                    .googlePlaceId(googlePlaceId)
                    .category(null)
                    .build();
            Place updatedPlace = Place.builder()
                    .id(placeId)
                    .name("Existing Place")
                    .googlePlaceId(googlePlaceId)
                    .category("restaurant")
                    .build();
            when(placeRepository.findByGooglePlaceId(googlePlaceId))
                    .thenReturn(Optional.of(existingPlace));
            when(placeRepository.save(existingPlace)).thenReturn(updatedPlace);

            UUID result = placeService.findOrCreate(googlePlaceId, "Existing Place",
                    "456 Test Ave", 25.0, 121.5, "RESTAURANT");

            assertThat(result).isEqualTo(placeId);
            verify(placeRepository).findByGooglePlaceId(googlePlaceId);
            verify(placeRepository).save(existingPlace);
        }

        @Test
        @DisplayName("should create new place when googlePlaceId not found in DB")
        void findOrCreate_newGooglePlaceId_shouldCreateNew() {
            String googlePlaceId = "ChIJ_NEW_PLACE";
            Place savedPlace = Place.builder()
                    .id(placeId)
                    .name("New Place")
                    .googlePlaceId(googlePlaceId)
                    .category("transit_station")
                    .build();
            when(placeRepository.findByGooglePlaceId(googlePlaceId))
                    .thenReturn(Optional.empty());
            when(placeRepository.save(any(Place.class))).thenReturn(savedPlace);

            UUID result = placeService.findOrCreate(googlePlaceId, "New Place",
                    "789 New St", 25.1, 121.6, "TRANSPORT");

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(placeId);
            verify(placeRepository).findByGooglePlaceId(googlePlaceId);
            verify(placeRepository).save(any(Place.class));
        }

        @Test
        @DisplayName("should default latitude and longitude to 0.0 when null")
        void findOrCreate_nullCoordinates_shouldDefaultToZero() {
            when(placeRepository.save(any(Place.class))).thenAnswer(invocation -> {
                Place p = invocation.getArgument(0);
                assertThat(p.getLatitude()).isEqualTo(0.0);
                assertThat(p.getLongitude()).isEqualTo(0.0);
                return p;
            });

            placeService.findOrCreate(null, "No Coords Place", "Unknown",
                    null, null, "ATTRACTION");

            verify(placeRepository).save(any(Place.class));
        }
    }

    @Nested
    @DisplayName("mapTypeToCategory")
    class MapTypeToCategoryTests {

        @Test
        @DisplayName("RESTAURANT -> restaurant")
        void restaurant() {
            assertThat(PlaceService.mapTypeToCategory("RESTAURANT")).isEqualTo("restaurant");
        }

        @Test
        @DisplayName("TRANSPORT -> transit_station")
        void transport() {
            assertThat(PlaceService.mapTypeToCategory("TRANSPORT")).isEqualTo("transit_station");
        }

        @Test
        @DisplayName("ACCOMMODATION -> lodging")
        void accommodation() {
            assertThat(PlaceService.mapTypeToCategory("ACCOMMODATION")).isEqualTo("lodging");
        }

        @Test
        @DisplayName("ATTRACTION -> null")
        void attraction() {
            assertThat(PlaceService.mapTypeToCategory("ATTRACTION")).isNull();
        }

        @Test
        @DisplayName("null -> null")
        void nullType() {
            assertThat(PlaceService.mapTypeToCategory(null)).isNull();
        }

        @Test
        @DisplayName("unknown type -> null")
        void unknownType() {
            assertThat(PlaceService.mapTypeToCategory("SHOPPING")).isNull();
        }
    }
}
