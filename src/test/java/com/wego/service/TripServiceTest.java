package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreateTripRequest;
import com.wego.dto.request.UpdateTripRequest;
import com.wego.dto.response.TripResponse;
import com.wego.entity.Role;
import com.wego.entity.Trip;
import com.wego.entity.TripMember;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.ValidationException;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TripService.
 *
 * Covers test cases: T-001 to T-050
 */
@Tag("fast")
@ExtendWith(MockitoExtension.class)
@DisplayName("TripService Unit Tests")
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionChecker permissionChecker;

    @InjectMocks
    private TripService tripService;

    private User testUser;
    private UUID tripId;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .nickname("Test User")
                .provider("google")
                .providerId("test-id")
                .build();

        tripId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Create Trip")
    class CreateTrip {

        @Test
        @DisplayName("T-001: Should create trip with valid input")
        void createTrip_withValidInput_shouldReturnCreatedTrip() {
            CreateTripRequest request = CreateTripRequest.builder()
                    .title("東京行")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();

            when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
                Trip trip = invocation.getArgument(0);
                trip.setId(tripId);
                return trip;
            });

            when(tripMemberRepository.save(any(TripMember.class))).thenAnswer(invocation -> {
                TripMember member = invocation.getArgument(0);
                member.setId(UUID.randomUUID());
                return member;
            });

            TripResponse response = tripService.createTrip(request, testUser);

            assertNotNull(response);
            assertEquals("東京行", response.getTitle());
            assertEquals(1, response.getMemberCount());
            assertEquals(Role.OWNER, response.getCurrentUserRole());

            // Verify trip was saved
            ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
            verify(tripRepository).save(tripCaptor.capture());
            assertEquals(testUser.getId(), tripCaptor.getValue().getOwnerId());

            // Verify owner membership was created
            ArgumentCaptor<TripMember> memberCaptor = ArgumentCaptor.forClass(TripMember.class);
            verify(tripMemberRepository).save(memberCaptor.capture());
            assertEquals(Role.OWNER, memberCaptor.getValue().getRole());
        }

        @Test
        @DisplayName("T-004: Should reject when end date before start date")
        void createTrip_endDateBeforeStartDate_shouldThrowValidationException() {
            CreateTripRequest request = CreateTripRequest.builder()
                    .title("Test")
                    .startDate(LocalDate.now().plusDays(5))
                    .endDate(LocalDate.now().plusDays(1))
                    .build();

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> tripService.createTrip(request, testUser));

            assertEquals("INVALID_DATE_RANGE", exception.getErrorCode());
        }

        @Test
        @DisplayName("Should default currency to TWD")
        void createTrip_withoutCurrency_shouldDefaultToTWD() {
            CreateTripRequest request = CreateTripRequest.builder()
                    .title("Test")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(3))
                    .build();

            when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
                Trip trip = invocation.getArgument(0);
                trip.setId(tripId);
                return trip;
            });
            when(tripMemberRepository.save(any(TripMember.class))).thenReturn(new TripMember());

            TripResponse response = tripService.createTrip(request, testUser);

            assertEquals("TWD", response.getBaseCurrency());
        }
    }

    @Nested
    @DisplayName("Get Trip")
    class GetTrip {

        @Test
        @DisplayName("T-012: Should return trip for member")
        void getTrip_asMember_shouldReturnTrip() {
            Trip trip = Trip.builder()
                    .id(tripId)
                    .title("Test Trip")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .ownerId(testUser.getId())
                    .build();

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(permissionChecker.canView(tripId, testUser.getId())).thenReturn(true);
            when(permissionChecker.getRole(tripId, testUser.getId())).thenReturn(Optional.of(Role.OWNER));
            when(tripMemberRepository.countByTripId(tripId)).thenReturn(1L);
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(Collections.emptyList());

            TripResponse response = tripService.getTrip(tripId, testUser.getId());

            assertNotNull(response);
            assertEquals("Test Trip", response.getTitle());
        }

        @Test
        @DisplayName("T-013: Should reject non-member")
        void getTrip_asNonMember_shouldThrowForbiddenException() {
            Trip trip = Trip.builder()
                    .id(tripId)
                    .title("Test Trip")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .ownerId(UUID.randomUUID())
                    .build();

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(permissionChecker.canView(tripId, testUser.getId())).thenReturn(false);

            assertThrows(ForbiddenException.class,
                    () -> tripService.getTrip(tripId, testUser.getId()));
        }

        @Test
        @DisplayName("T-014: Should return 404 for non-existent trip")
        void getTrip_nonExistent_shouldThrowNotFoundException() {
            when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

            ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                    () -> tripService.getTrip(tripId, testUser.getId()));

            assertEquals("TRIP_NOT_FOUND", exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Update Trip")
    class UpdateTrip {

        @Test
        @DisplayName("T-020: Owner can update trip")
        void updateTrip_asOwner_shouldUpdateTrip() {
            Trip trip = Trip.builder()
                    .id(tripId)
                    .title("Old Title")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .ownerId(testUser.getId())
                    .build();

            UpdateTripRequest request = UpdateTripRequest.builder()
                    .title("New Title")
                    .build();

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(permissionChecker.canDelete(tripId, testUser.getId())).thenReturn(true);
            when(tripRepository.save(any(Trip.class))).thenReturn(trip);
            when(tripMemberRepository.countByTripId(tripId)).thenReturn(1L);

            TripResponse response = tripService.updateTrip(tripId, request, testUser.getId());

            assertEquals("New Title", response.getTitle());
        }

        @Test
        @DisplayName("T-021: Editor cannot update basic info")
        void updateTrip_asEditor_shouldThrowForbiddenException() {
            Trip trip = Trip.builder()
                    .id(tripId)
                    .title("Test")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .ownerId(UUID.randomUUID())
                    .build();

            UpdateTripRequest request = UpdateTripRequest.builder()
                    .title("New Title")
                    .build();

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(permissionChecker.canDelete(tripId, testUser.getId())).thenReturn(false);

            assertThrows(ForbiddenException.class,
                    () -> tripService.updateTrip(tripId, request, testUser.getId()));
        }
    }

    @Nested
    @DisplayName("Delete Trip")
    class DeleteTrip {

        @Test
        @DisplayName("T-030: Owner can delete trip")
        void deleteTrip_asOwner_shouldDeleteTrip() {
            Trip trip = Trip.builder()
                    .id(tripId)
                    .title("Test")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .ownerId(testUser.getId())
                    .build();

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(permissionChecker.canDelete(tripId, testUser.getId())).thenReturn(true);

            tripService.deleteTrip(tripId, testUser.getId());

            verify(tripMemberRepository).deleteByTripId(tripId);
            verify(tripRepository).delete(trip);
        }

        @Test
        @DisplayName("T-031: Editor cannot delete trip")
        void deleteTrip_asEditor_shouldThrowForbiddenException() {
            Trip trip = Trip.builder()
                    .id(tripId)
                    .title("Test")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .ownerId(UUID.randomUUID())
                    .build();

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(permissionChecker.canDelete(tripId, testUser.getId())).thenReturn(false);

            assertThrows(ForbiddenException.class,
                    () -> tripService.deleteTrip(tripId, testUser.getId()));
        }
    }

    @Nested
    @DisplayName("Member Management")
    class MemberManagement {

        @Test
        @DisplayName("T-045: Should reject adding duplicate member")
        void addMember_duplicate_shouldThrowValidationException() {
            Trip trip = Trip.builder()
                    .id(tripId)
                    .title("Test")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .ownerId(UUID.randomUUID())
                    .build();

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, testUser.getId())).thenReturn(true);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> tripService.addMember(tripId, testUser.getId(), Role.EDITOR));

            assertEquals("DUPLICATE_MEMBER", exception.getErrorCode());
        }

        @Test
        @DisplayName("T-046: Should reject when member limit exceeded")
        void addMember_limitExceeded_shouldThrowValidationException() {
            Trip trip = Trip.builder()
                    .id(tripId)
                    .title("Test")
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(5))
                    .ownerId(UUID.randomUUID())
                    .build();

            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, testUser.getId())).thenReturn(false);
            when(tripMemberRepository.countByTripId(tripId)).thenReturn(10L);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> tripService.addMember(tripId, testUser.getId(), Role.EDITOR));

            assertEquals("MEMBER_LIMIT_EXCEEDED", exception.getErrorCode());
        }

        @Test
        @DisplayName("T-047: Owner can remove member")
        void removeMember_asOwner_shouldRemoveMember() {
            UUID targetUserId = UUID.randomUUID();
            TripMember member = TripMember.builder()
                    .tripId(tripId)
                    .userId(targetUserId)
                    .role(Role.EDITOR)
                    .build();

            when(permissionChecker.canManageMembers(tripId, testUser.getId())).thenReturn(true);
            when(tripMemberRepository.findByTripIdAndUserId(tripId, targetUserId))
                    .thenReturn(Optional.of(member));

            tripService.removeMember(tripId, targetUserId, testUser.getId());

            verify(tripMemberRepository).delete(member);
        }

        @Test
        @DisplayName("T-048: Editor cannot remove member")
        void removeMember_asEditor_shouldThrowForbiddenException() {
            when(permissionChecker.canManageMembers(tripId, testUser.getId())).thenReturn(false);

            assertThrows(ForbiddenException.class,
                    () -> tripService.removeMember(tripId, UUID.randomUUID(), testUser.getId()));
        }

        @Test
        @DisplayName("T-049: Owner can change member role")
        void changeMemberRole_asOwner_shouldChangeRole() {
            UUID targetUserId = UUID.randomUUID();
            TripMember member = TripMember.builder()
                    .tripId(tripId)
                    .userId(targetUserId)
                    .role(Role.EDITOR)
                    .build();

            when(permissionChecker.canManageMembers(tripId, testUser.getId())).thenReturn(true);
            when(tripMemberRepository.findByTripIdAndUserId(tripId, targetUserId))
                    .thenReturn(Optional.of(member));
            when(tripMemberRepository.save(any(TripMember.class))).thenReturn(member);

            tripService.changeMemberRole(tripId, targetUserId, Role.VIEWER, testUser.getId());

            assertEquals(Role.VIEWER, member.getRole());
            verify(tripMemberRepository).save(member);
        }

        @Test
        @DisplayName("T-050: Cannot change role to OWNER")
        void changeMemberRole_toOwner_shouldThrowValidationException() {
            when(permissionChecker.canManageMembers(tripId, testUser.getId())).thenReturn(true);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> tripService.changeMemberRole(tripId, UUID.randomUUID(), Role.OWNER, testUser.getId()));

            assertEquals("INVALID_ROLE_CHANGE", exception.getErrorCode());
        }
    }
}
