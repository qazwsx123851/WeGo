package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreateInviteLinkRequest;
import com.wego.dto.response.InviteLinkResponse;
import com.wego.entity.InviteLink;
import com.wego.entity.Role;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ValidationException;
import com.wego.repository.GhostMemberRepository;
import com.wego.repository.InviteLinkRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InviteLinkService.
 *
 * Covers test cases: T-040 to T-050
 */
@Tag("fast")
@ExtendWith(MockitoExtension.class)
@DisplayName("InviteLinkService Unit Tests")
class InviteLinkServiceTest {

    @Mock
    private InviteLinkRepository inviteLinkRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private GhostMemberRepository ghostMemberRepository;

    @Mock
    private TripService tripService;

    @Mock
    private PermissionChecker permissionChecker;

    @InjectMocks
    private InviteLinkService inviteLinkService;

    private UUID tripId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(inviteLinkService, "baseUrl", "http://localhost:8080");
    }

    @Nested
    @DisplayName("Create Invite Link")
    class CreateInviteLink {

        @Test
        @DisplayName("T-040: Owner can create invite link")
        void createInviteLink_asOwner_shouldCreateLink() {
            CreateInviteLinkRequest request = CreateInviteLinkRequest.builder()
                    .role(Role.EDITOR)
                    .expiryDays(7)
                    .build();

            when(tripRepository.existsById(tripId)).thenReturn(true);
            when(permissionChecker.canInvite(tripId, userId)).thenReturn(true);
            when(inviteLinkRepository.save(any(InviteLink.class))).thenAnswer(invocation -> {
                InviteLink link = invocation.getArgument(0);
                link.setId(UUID.randomUUID());
                return link;
            });

            InviteLinkResponse response = inviteLinkService.createInviteLink(tripId, request, userId);

            assertNotNull(response);
            assertEquals(Role.EDITOR, response.getRole());
            assertNotNull(response.getToken());
            assertTrue(response.getInviteUrl().contains(response.getToken()));
        }

        @Test
        @DisplayName("T-041: Editor can create invite link")
        void createInviteLink_asEditor_shouldCreateLink() {
            CreateInviteLinkRequest request = CreateInviteLinkRequest.builder()
                    .role(Role.VIEWER)
                    .expiryDays(1)
                    .build();

            when(tripRepository.existsById(tripId)).thenReturn(true);
            when(permissionChecker.canInvite(tripId, userId)).thenReturn(true);
            when(inviteLinkRepository.save(any(InviteLink.class))).thenAnswer(invocation -> {
                InviteLink link = invocation.getArgument(0);
                link.setId(UUID.randomUUID());
                return link;
            });

            InviteLinkResponse response = inviteLinkService.createInviteLink(tripId, request, userId);

            assertNotNull(response);
            assertEquals(Role.VIEWER, response.getRole());
        }

        @Test
        @DisplayName("T-042: Viewer cannot create invite link")
        void createInviteLink_asViewer_shouldThrowForbiddenException() {
            CreateInviteLinkRequest request = CreateInviteLinkRequest.builder()
                    .role(Role.EDITOR)
                    .build();

            when(tripRepository.existsById(tripId)).thenReturn(true);
            when(permissionChecker.canInvite(tripId, userId)).thenReturn(false);

            assertThrows(ForbiddenException.class,
                    () -> inviteLinkService.createInviteLink(tripId, request, userId));
        }

        @Test
        @DisplayName("Cannot create invite with OWNER role")
        void createInviteLink_withOwnerRole_shouldThrowValidationException() {
            CreateInviteLinkRequest request = CreateInviteLinkRequest.builder()
                    .role(Role.OWNER)
                    .build();

            when(tripRepository.existsById(tripId)).thenReturn(true);
            when(permissionChecker.canInvite(tripId, userId)).thenReturn(true);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> inviteLinkService.createInviteLink(tripId, request, userId));

            assertEquals("INVALID_ROLE", exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Accept Invite")
    class AcceptInvite {

        @Test
        @DisplayName("T-043: Should accept valid invite link")
        void acceptInvite_validLink_shouldAddMember() {
            String token = "valid-token";
            InviteLink link = InviteLink.builder()
                    .id(UUID.randomUUID())
                    .token(token)
                    .tripId(tripId)
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            when(inviteLinkRepository.findByToken(token)).thenReturn(Optional.of(link));
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, userId)).thenReturn(false);
            when(tripMemberRepository.countByTripId(tripId)).thenReturn(5L);
            when(ghostMemberRepository.countByTripIdAndMergedToUserIdIsNull(tripId)).thenReturn(0L);
            when(inviteLinkRepository.save(any(InviteLink.class))).thenReturn(link);

            UUID result = inviteLinkService.acceptInvite(token, userId);

            assertEquals(tripId, result);
            verify(tripService).addMember(tripId, userId, Role.EDITOR);
            assertEquals(1, link.getUseCount());
        }

        @Test
        @DisplayName("T-044: Should reject expired invite link")
        void acceptInvite_expiredLink_shouldThrowValidationException() {
            String token = "expired-token";
            InviteLink link = InviteLink.builder()
                    .id(UUID.randomUUID())
                    .token(token)
                    .tripId(tripId)
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            when(inviteLinkRepository.findByToken(token)).thenReturn(Optional.of(link));

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> inviteLinkService.acceptInvite(token, userId));

            assertEquals("INVALID_INVITE_LINK", exception.getErrorCode());
        }

        @Test
        @DisplayName("T-045: Should reject if already a member")
        void acceptInvite_alreadyMember_shouldThrowValidationException() {
            String token = "valid-token";
            InviteLink link = InviteLink.builder()
                    .id(UUID.randomUUID())
                    .token(token)
                    .tripId(tripId)
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            when(inviteLinkRepository.findByToken(token)).thenReturn(Optional.of(link));
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, userId)).thenReturn(true);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> inviteLinkService.acceptInvite(token, userId));

            assertEquals("DUPLICATE_MEMBER", exception.getErrorCode());
        }

        @Test
        @DisplayName("T-046: Should reject if member limit exceeded")
        void acceptInvite_memberLimitExceeded_shouldThrowValidationException() {
            String token = "valid-token";
            InviteLink link = InviteLink.builder()
                    .id(UUID.randomUUID())
                    .token(token)
                    .tripId(tripId)
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            when(inviteLinkRepository.findByToken(token)).thenReturn(Optional.of(link));
            when(tripMemberRepository.existsByTripIdAndUserId(tripId, userId)).thenReturn(false);
            when(tripMemberRepository.countByTripId(tripId)).thenReturn(10L);
            when(ghostMemberRepository.countByTripIdAndMergedToUserIdIsNull(tripId)).thenReturn(0L);

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> inviteLinkService.acceptInvite(token, userId));

            assertEquals("MEMBER_LIMIT_EXCEEDED", exception.getErrorCode());
        }

        @Test
        @DisplayName("Should reject invalid token")
        void acceptInvite_invalidToken_shouldThrowValidationException() {
            String token = "invalid-token";

            when(inviteLinkRepository.findByToken(token)).thenReturn(Optional.empty());

            ValidationException exception = assertThrows(ValidationException.class,
                    () -> inviteLinkService.acceptInvite(token, userId));

            assertEquals("INVALID_INVITE_LINK", exception.getErrorCode());
        }
    }

    @Nested
    @DisplayName("Delete Invite Link")
    class DeleteInviteLink {

        @Test
        @DisplayName("Owner can delete invite link")
        void deleteInviteLink_asOwner_shouldDeleteLink() {
            UUID linkId = UUID.randomUUID();
            InviteLink link = InviteLink.builder()
                    .id(linkId)
                    .tripId(tripId)
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            when(inviteLinkRepository.findById(linkId)).thenReturn(Optional.of(link));
            when(permissionChecker.canManageMembers(tripId, userId)).thenReturn(true);

            inviteLinkService.deleteInviteLink(linkId, userId);

            verify(inviteLinkRepository).delete(link);
        }

        @Test
        @DisplayName("Editor cannot delete invite link")
        void deleteInviteLink_asEditor_shouldThrowForbiddenException() {
            UUID linkId = UUID.randomUUID();
            InviteLink link = InviteLink.builder()
                    .id(linkId)
                    .tripId(tripId)
                    .role(Role.EDITOR)
                    .expiresAt(Instant.now().plus(7, ChronoUnit.DAYS))
                    .createdBy(UUID.randomUUID())
                    .build();

            when(inviteLinkRepository.findById(linkId)).thenReturn(Optional.of(link));
            when(permissionChecker.canManageMembers(tripId, userId)).thenReturn(false);

            assertThrows(ForbiddenException.class,
                    () -> inviteLinkService.deleteInviteLink(linkId, userId));
        }
    }
}
