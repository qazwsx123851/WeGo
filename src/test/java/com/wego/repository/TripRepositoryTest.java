package com.wego.repository;

import com.wego.entity.Role;
import com.wego.entity.Trip;
import com.wego.entity.TripMember;
import com.wego.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TripRepository.
 */
@Tag("fast")
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TripRepository Integration Tests")
class TripRepositoryTest {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripMemberRepository tripMemberRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        tripMemberRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("test@example.com")
                .nickname("Test User")
                .provider("google")
                .providerId("test-provider-id")
                .build());

        anotherUser = userRepository.save(User.builder()
                .email("another@example.com")
                .nickname("Another User")
                .provider("google")
                .providerId("another-provider-id")
                .build());
    }

    @Nested
    @DisplayName("Basic CRUD Operations")
    class BasicCRUDOperations {

        @Test
        @DisplayName("Should save and retrieve trip")
        void save_shouldPersistTrip() {
            Trip trip = Trip.builder()
                    .title("東京行")
                    .description("五天四夜")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .ownerId(testUser.getId())
                    .build();

            Trip saved = tripRepository.save(trip);

            assertNotNull(saved.getId());
            assertEquals("東京行", saved.getTitle());
        }

        @Test
        @DisplayName("Should find trip by ID")
        void findById_existingTrip_shouldReturnTrip() {
            Trip trip = tripRepository.save(Trip.builder()
                    .title("Test Trip")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .ownerId(testUser.getId())
                    .build());

            Optional<Trip> found = tripRepository.findById(trip.getId());

            assertTrue(found.isPresent());
            assertEquals("Test Trip", found.get().getTitle());
        }

        @Test
        @DisplayName("Should return empty for non-existent ID")
        void findById_nonExistentTrip_shouldReturnEmpty() {
            Optional<Trip> found = tripRepository.findById(UUID.randomUUID());

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("Should delete trip")
        void delete_shouldRemoveTrip() {
            Trip trip = tripRepository.save(Trip.builder()
                    .title("To Delete")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(2))
                    .ownerId(testUser.getId())
                    .build());

            tripRepository.deleteById(trip.getId());

            assertFalse(tripRepository.existsById(trip.getId()));
        }
    }

    @Nested
    @DisplayName("Find By Owner")
    class FindByOwner {

        @Test
        @DisplayName("Should find trips by owner ID")
        void findByOwnerId_shouldReturnOwnerTrips() {
            tripRepository.save(Trip.builder()
                    .title("Trip 1")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(2))
                    .ownerId(testUser.getId())
                    .build());

            tripRepository.save(Trip.builder()
                    .title("Trip 2")
                    .startDate(LocalDate.now().plusDays(3))
                    .endDate(LocalDate.now().plusDays(4))
                    .ownerId(testUser.getId())
                    .build());

            tripRepository.save(Trip.builder()
                    .title("Other Trip")
                    .startDate(LocalDate.now().plusDays(5))
                    .endDate(LocalDate.now().plusDays(6))
                    .ownerId(anotherUser.getId())
                    .build());

            List<Trip> trips = tripRepository.findByOwnerId(testUser.getId());

            assertEquals(2, trips.size());
            assertTrue(trips.stream().allMatch(t -> t.getOwnerId().equals(testUser.getId())));
        }

        @Test
        @DisplayName("Should return empty list for owner with no trips")
        void findByOwnerId_noTrips_shouldReturnEmptyList() {
            List<Trip> trips = tripRepository.findByOwnerId(UUID.randomUUID());

            assertTrue(trips.isEmpty());
        }

        @Test
        @DisplayName("Should support pagination")
        void findByOwnerId_withPagination_shouldReturnPage() {
            for (int i = 0; i < 5; i++) {
                tripRepository.save(Trip.builder()
                        .title("Trip " + i)
                        .startDate(LocalDate.now().plusDays(i + 1))
                        .endDate(LocalDate.now().plusDays(i + 2))
                        .ownerId(testUser.getId())
                        .build());
            }

            Page<Trip> page = tripRepository.findByOwnerId(
                    testUser.getId(),
                    PageRequest.of(0, 2, Sort.by("startDate")));

            assertEquals(2, page.getContent().size());
            assertEquals(5, page.getTotalElements());
            assertEquals(3, page.getTotalPages());
        }
    }

    @Nested
    @DisplayName("Find Trips By Member")
    class FindTripsByMember {

        @Test
        @DisplayName("Should find trips where user is a member")
        void findTripsByMemberId_shouldReturnMemberTrips() {
            Trip trip1 = tripRepository.save(Trip.builder()
                    .title("Trip 1")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(2))
                    .ownerId(anotherUser.getId())
                    .build());

            Trip trip2 = tripRepository.save(Trip.builder()
                    .title("Trip 2")
                    .startDate(LocalDate.now().plusDays(3))
                    .endDate(LocalDate.now().plusDays(4))
                    .ownerId(anotherUser.getId())
                    .build());

            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip1.getId())
                    .userId(testUser.getId())
                    .role(Role.EDITOR)
                    .build());

            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip2.getId())
                    .userId(testUser.getId())
                    .role(Role.VIEWER)
                    .build());

            List<Trip> trips = tripRepository.findTripsByMemberId(testUser.getId());

            assertEquals(2, trips.size());
        }

        @Test
        @DisplayName("Should return empty for non-member")
        void findTripsByMemberId_nonMember_shouldReturnEmpty() {
            tripRepository.save(Trip.builder()
                    .title("Trip")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(2))
                    .ownerId(anotherUser.getId())
                    .build());

            List<Trip> trips = tripRepository.findTripsByMemberId(testUser.getId());

            assertTrue(trips.isEmpty());
        }
    }

    @Nested
    @DisplayName("Exists and Count")
    class ExistsAndCount {

        @Test
        @DisplayName("Should check existence by ID and owner")
        void existsByIdAndOwnerId_shouldReturnCorrectly() {
            Trip trip = tripRepository.save(Trip.builder()
                    .title("Test")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(2))
                    .ownerId(testUser.getId())
                    .build());

            assertTrue(tripRepository.existsByIdAndOwnerId(trip.getId(), testUser.getId()));
            assertFalse(tripRepository.existsByIdAndOwnerId(trip.getId(), anotherUser.getId()));
            assertFalse(tripRepository.existsByIdAndOwnerId(UUID.randomUUID(), testUser.getId()));
        }

        @Test
        @DisplayName("Should count trips by owner")
        void countByOwnerId_shouldReturnCorrectCount() {
            tripRepository.save(Trip.builder()
                    .title("Trip 1")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(2))
                    .ownerId(testUser.getId())
                    .build());

            tripRepository.save(Trip.builder()
                    .title("Trip 2")
                    .startDate(LocalDate.now().plusDays(3))
                    .endDate(LocalDate.now().plusDays(4))
                    .ownerId(testUser.getId())
                    .build());

            assertEquals(2, tripRepository.countByOwnerId(testUser.getId()));
            assertEquals(0, tripRepository.countByOwnerId(anotherUser.getId()));
        }
    }
}
