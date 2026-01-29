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
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TripMemberRepository.
 */
@Tag("fast")
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("TripMemberRepository Integration Tests")
class TripMemberRepositoryTest {

    @Autowired
    private TripMemberRepository tripMemberRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    private User owner;
    private User editor;
    private User viewer;
    private Trip trip;

    @BeforeEach
    void setUp() {
        tripMemberRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.deleteAll();

        owner = userRepository.save(User.builder()
                .email("owner@example.com")
                .nickname("Owner")
                .provider("google")
                .providerId("owner-id")
                .build());

        editor = userRepository.save(User.builder()
                .email("editor@example.com")
                .nickname("Editor")
                .provider("google")
                .providerId("editor-id")
                .build());

        viewer = userRepository.save(User.builder()
                .email("viewer@example.com")
                .nickname("Viewer")
                .provider("google")
                .providerId("viewer-id")
                .build());

        trip = tripRepository.save(Trip.builder()
                .title("Test Trip")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .ownerId(owner.getId())
                .build());
    }

    @Nested
    @DisplayName("Find By Trip And User")
    class FindByTripAndUser {

        @Test
        @DisplayName("Should find existing membership")
        void findByTripIdAndUserId_existingMember_shouldReturnMember() {
            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(owner.getId())
                    .role(Role.OWNER)
                    .build());

            Optional<TripMember> found = tripMemberRepository.findByTripIdAndUserId(
                    trip.getId(), owner.getId());

            assertTrue(found.isPresent());
            assertEquals(Role.OWNER, found.get().getRole());
        }

        @Test
        @DisplayName("Should return empty for non-member")
        void findByTripIdAndUserId_nonMember_shouldReturnEmpty() {
            Optional<TripMember> found = tripMemberRepository.findByTripIdAndUserId(
                    trip.getId(), viewer.getId());

            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find By Trip")
    class FindByTrip {

        @Test
        @DisplayName("Should find all members of a trip")
        void findByTripId_shouldReturnAllMembers() {
            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(owner.getId())
                    .role(Role.OWNER)
                    .build());

            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(editor.getId())
                    .role(Role.EDITOR)
                    .build());

            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(viewer.getId())
                    .role(Role.VIEWER)
                    .build());

            List<TripMember> members = tripMemberRepository.findByTripId(trip.getId());

            assertEquals(3, members.size());
        }

        @Test
        @DisplayName("Should return empty for trip with no members")
        void findByTripId_noMembers_shouldReturnEmpty() {
            List<TripMember> members = tripMemberRepository.findByTripId(UUID.randomUUID());

            assertTrue(members.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find By User")
    class FindByUser {

        @Test
        @DisplayName("Should find all memberships for a user")
        void findByUserId_shouldReturnAllMemberships() {
            Trip trip2 = tripRepository.save(Trip.builder()
                    .title("Another Trip")
                    .startDate(LocalDate.now().plusDays(10))
                    .endDate(LocalDate.now().plusDays(15))
                    .ownerId(owner.getId())
                    .build());

            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(editor.getId())
                    .role(Role.EDITOR)
                    .build());

            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip2.getId())
                    .userId(editor.getId())
                    .role(Role.VIEWER)
                    .build());

            List<TripMember> memberships = tripMemberRepository.findByUserId(editor.getId());

            assertEquals(2, memberships.size());
        }
    }

    @Nested
    @DisplayName("Exists and Count")
    class ExistsAndCount {

        @Test
        @DisplayName("Should check membership existence")
        void existsByTripIdAndUserId_shouldReturnCorrectly() {
            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(owner.getId())
                    .role(Role.OWNER)
                    .build());

            assertTrue(tripMemberRepository.existsByTripIdAndUserId(trip.getId(), owner.getId()));
            assertFalse(tripMemberRepository.existsByTripIdAndUserId(trip.getId(), editor.getId()));
        }

        @Test
        @DisplayName("Should count members of a trip")
        void countByTripId_shouldReturnCorrectCount() {
            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(owner.getId())
                    .role(Role.OWNER)
                    .build());

            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(editor.getId())
                    .role(Role.EDITOR)
                    .build());

            assertEquals(2, tripMemberRepository.countByTripId(trip.getId()));
        }
    }

    @Nested
    @DisplayName("Find By Role")
    class FindByRole {

        @Test
        @DisplayName("Should find owner of a trip")
        void findByTripIdAndRole_owner_shouldReturnOwner() {
            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(owner.getId())
                    .role(Role.OWNER)
                    .build());

            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(editor.getId())
                    .role(Role.EDITOR)
                    .build());

            Optional<TripMember> found = tripMemberRepository.findByTripIdAndRole(
                    trip.getId(), Role.OWNER);

            assertTrue(found.isPresent());
            assertEquals(owner.getId(), found.get().getUserId());
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should delete all members of a trip")
        void deleteByTripId_shouldDeleteAllMembers() {
            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(owner.getId())
                    .role(Role.OWNER)
                    .build());

            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(editor.getId())
                    .role(Role.EDITOR)
                    .build());

            tripMemberRepository.deleteByTripId(trip.getId());

            assertEquals(0, tripMemberRepository.countByTripId(trip.getId()));
        }

        @Test
        @DisplayName("Should delete specific membership")
        void deleteByTripIdAndUserId_shouldDeleteMember() {
            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(owner.getId())
                    .role(Role.OWNER)
                    .build());

            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(editor.getId())
                    .role(Role.EDITOR)
                    .build());

            tripMemberRepository.deleteByTripIdAndUserId(trip.getId(), editor.getId());

            assertEquals(1, tripMemberRepository.countByTripId(trip.getId()));
            assertFalse(tripMemberRepository.existsByTripIdAndUserId(trip.getId(), editor.getId()));
            assertTrue(tripMemberRepository.existsByTripIdAndUserId(trip.getId(), owner.getId()));
        }
    }

    @Nested
    @DisplayName("Unique Constraint")
    class UniqueConstraint {

        @Test
        @DisplayName("T-045: Should not allow duplicate membership")
        void save_duplicateMembership_shouldFail() {
            tripMemberRepository.save(TripMember.builder()
                    .tripId(trip.getId())
                    .userId(owner.getId())
                    .role(Role.OWNER)
                    .build());

            TripMember duplicate = TripMember.builder()
                    .tripId(trip.getId())
                    .userId(owner.getId())
                    .role(Role.EDITOR)
                    .build();

            assertThrows(Exception.class, () -> {
                tripMemberRepository.saveAndFlush(duplicate);
            });
        }
    }
}
