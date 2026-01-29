package com.wego.repository;

import com.wego.entity.InviteLink;
import com.wego.entity.Role;
import com.wego.entity.Trip;
import com.wego.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for InviteLinkRepository.
 */
@Tag("fast")
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("InviteLinkRepository Integration Tests")
class InviteLinkRepositoryTest {

    @Autowired
    private InviteLinkRepository inviteLinkRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private Trip trip;

    @BeforeEach
    void setUp() {
        inviteLinkRepository.deleteAll();
        tripRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(User.builder()
                .email("test@example.com")
                .nickname("Test User")
                .provider("google")
                .providerId("test-id")
                .build());

        trip = tripRepository.save(Trip.builder()
                .title("Test Trip")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .ownerId(user.getId())
                .build());
    }

    @Nested
    @DisplayName("Find By Token")
    class FindByToken {

        @Test
        @DisplayName("Should find invite link by token")
        void findByToken_existingToken_shouldReturnLink() {
            InviteLink link = inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            Optional<InviteLink> found = inviteLinkRepository.findByToken(link.getToken());

            assertTrue(found.isPresent());
            assertEquals(Role.EDITOR, found.get().getRole());
        }

        @Test
        @DisplayName("Should return empty for non-existent token")
        void findByToken_nonExistentToken_shouldReturnEmpty() {
            Optional<InviteLink> found = inviteLinkRepository.findByToken("non-existent-token");

            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("Find By Trip")
    class FindByTrip {

        @Test
        @DisplayName("Should find all invite links for a trip")
        void findByTripId_shouldReturnAllLinks() {
            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.VIEWER)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            List<InviteLink> links = inviteLinkRepository.findByTripId(trip.getId());

            assertEquals(2, links.size());
        }

        @Test
        @DisplayName("Should find only active (non-expired) links")
        void findActiveByTripId_shouldReturnOnlyActiveLinks() {
            // Active link
            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            // Expired link
            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.VIEWER)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            List<InviteLink> activeLinks = inviteLinkRepository.findActiveByTripId(
                    trip.getId(), Instant.now());

            assertEquals(1, activeLinks.size());
            assertEquals(Role.EDITOR, activeLinks.get(0).getRole());
        }
    }

    @Nested
    @DisplayName("Exists By Token")
    class ExistsByToken {

        @Test
        @DisplayName("Should check token existence")
        void existsByToken_shouldReturnCorrectly() {
            InviteLink link = inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            assertTrue(inviteLinkRepository.existsByToken(link.getToken()));
            assertFalse(inviteLinkRepository.existsByToken("non-existent"));
        }
    }

    @Nested
    @DisplayName("Count Active Links")
    class CountActiveLinks {

        @Test
        @DisplayName("Should count only active links")
        void countActiveByTripId_shouldCountOnlyActive() {
            // 2 active links
            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.VIEWER)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            // 1 expired link
            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            long count = inviteLinkRepository.countActiveByTripId(trip.getId(), Instant.now());

            assertEquals(2, count);
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should delete all links for a trip")
        void deleteByTripId_shouldDeleteAllLinks() {
            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.VIEWER)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            inviteLinkRepository.deleteByTripId(trip.getId());

            assertTrue(inviteLinkRepository.findByTripId(trip.getId()).isEmpty());
        }

        @Test
        @DisplayName("Should delete expired links")
        void deleteExpiredLinks_shouldDeleteOnlyExpired() {
            // Active link
            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            // Expired links
            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.VIEWER)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.VIEWER)
                    .expiresAt(Instant.now().minus(2, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            int deleted = inviteLinkRepository.deleteExpiredLinks(Instant.now());

            assertEquals(2, deleted);
            assertEquals(1, inviteLinkRepository.findByTripId(trip.getId()).size());
        }
    }

    @Nested
    @DisplayName("Token Uniqueness")
    class TokenUniqueness {

        @Test
        @DisplayName("Should not allow duplicate tokens")
        void save_duplicateToken_shouldFail() {
            String token = "unique-test-token-123456789012345";

            inviteLinkRepository.save(InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.EDITOR)
                    .token(token)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build());

            InviteLink duplicate = InviteLink.builder()
                    .tripId(trip.getId())
                    .role(Role.VIEWER)
                    .token(token)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .createdBy(user.getId())
                    .build();

            assertThrows(Exception.class, () -> {
                inviteLinkRepository.saveAndFlush(duplicate);
            });
        }
    }
}
