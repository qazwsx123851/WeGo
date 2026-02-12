package com.wego.service;

import com.wego.config.SupabaseProperties;
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
import com.wego.repository.ActivityRepository;
import com.wego.repository.DocumentRepository;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.InviteLinkRepository;
import com.wego.repository.TodoRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import com.wego.service.external.StorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for trip-related business logic.
 *
 * @contract
 *   - All methods that modify data are transactional
 *   - Permission checks are performed before modifications
 *   - Cascading deletes handled for trip-related entities
 *
 * @see Trip
 * @see TripMember
 * @see PermissionChecker
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private static final int MAX_MEMBERS_PER_TRIP = com.wego.domain.TripConstants.MAX_MEMBERS_PER_TRIP;
    private static final long MAX_COVER_IMAGE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Map<String, byte[][]> IMAGE_MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[][] {
                    {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0},
                    {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE1},
                    {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE8}
            },
            "image/png", new byte[][] {
                    {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
            },
            "image/webp", new byte[][] {
                    {0x52, 0x49, 0x46, 0x46} // RIFF header (first 4 bytes)
            }
    );

    private final TripRepository tripRepository;
    private final TripMemberRepository tripMemberRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final DocumentRepository documentRepository;
    private final TodoRepository todoRepository;
    private final InviteLinkRepository inviteLinkRepository;
    private final PermissionChecker permissionChecker;
    private final StorageClient storageClient;
    private final SupabaseProperties supabaseProperties;

    /**
     * Creates a new trip and sets the creator as owner.
     *
     * @contract
     *   - pre: request != null, user != null
     *   - pre: request.endDate >= request.startDate
     *   - post: Trip is persisted, TripMember with OWNER role is created
     *   - calledBy: TripApiController#createTrip
     *
     * @param request The trip creation request
     * @param user The creating user (will become owner)
     * @return The created trip response
     * @throws ValidationException if validation fails
     */
    @Transactional
    public TripResponse createTrip(CreateTripRequest request, User user) {
        validateTripDates(request.getStartDate(), request.getEndDate());

        Trip trip = Trip.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .baseCurrency(request.getBaseCurrency() != null ? request.getBaseCurrency() : "TWD")
                .coverImageUrl(request.getCoverImageUrl())
                .ownerId(user.getId())
                .build();

        trip = tripRepository.save(trip);
        log.info("Created trip: {} by user: {}", trip.getId(), user.getId());

        // Create owner membership
        TripMember ownerMember = TripMember.builder()
                .tripId(trip.getId())
                .userId(user.getId())
                .role(Role.OWNER)
                .build();

        tripMemberRepository.save(ownerMember);
        log.debug("Created owner membership for trip: {}", trip.getId());

        TripResponse response = TripResponse.fromEntity(trip);
        response.setMemberCount(1);
        response.setCurrentUserRole(Role.OWNER);

        return response;
    }

    /**
     * Gets a trip by ID with permission check.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: Returns trip if user is a member
     *   - calledBy: TripApiController#getTrip
     *
     * @param tripId The trip ID
     * @param userId The requesting user ID
     * @return The trip response
     * @throws ResourceNotFoundException if trip not found
     * @throws ForbiddenException if user is not a member
     */
    @Transactional(readOnly = true)
    public TripResponse getTrip(UUID tripId, UUID userId) {
        Trip trip = findTripById(tripId);

        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程");
        }

        TripResponse response = TripResponse.fromEntity(trip);
        response.setMemberCount((int) tripMemberRepository.countByTripId(tripId));
        response.setCurrentUserRole(permissionChecker.getRole(tripId, userId).orElse(null));
        response.setMembers(getMemberSummaries(tripId));

        return response;
    }

    /**
     * Gets all trips for a user (as member).
     *
     * @contract
     *   - pre: userId != null, pageable != null
     *   - post: Returns paginated list of trips
     *   - calledBy: TripApiController#getUserTrips
     *
     * @param userId The user ID
     * @param pageable Pagination parameters
     * @return Page of trip responses
     */
    @Transactional(readOnly = true)
    public Page<TripResponse> getUserTrips(UUID userId, Pageable pageable) {
        Page<Trip> tripPage = tripRepository.findTripsByMemberId(userId, pageable);

        List<UUID> tripIds = tripPage.getContent().stream()
                .map(Trip::getId)
                .toList();

        if (tripIds.isEmpty()) {
            return tripPage.map(TripResponse::fromEntity);
        }

        // Batch load all members for all trips (1 query instead of N)
        Map<UUID, List<TripMember>> membersByTripId = tripMemberRepository.findByTripIdIn(tripIds)
                .stream()
                .collect(Collectors.groupingBy(TripMember::getTripId));

        // Batch load all users referenced by members (1 query instead of N)
        Set<UUID> allUserIds = membersByTripId.values().stream()
                .flatMap(List::stream)
                .map(TripMember::getUserId)
                .collect(Collectors.toSet());
        Map<UUID, User> userMap = userRepository.findAllById(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, java.util.function.Function.identity(), (a, b) -> a));

        return tripPage.map(trip -> {
            TripResponse response = TripResponse.fromEntity(trip);
            List<TripMember> tripMembers = membersByTripId.getOrDefault(trip.getId(), List.of());

            List<TripResponse.MemberSummary> memberSummaries = tripMembers.stream()
                    .map(member -> {
                        User user = userMap.get(member.getUserId());
                        return TripResponse.MemberSummary.builder()
                                .userId(member.getUserId())
                                .nickname(user != null ? user.getNickname() : UNKNOWN_USER_NAME)
                                .avatarUrl(user != null ? user.getAvatarUrl() : null)
                                .role(member.getRole())
                                .build();
                    })
                    .collect(Collectors.toList());

            response.setMembers(memberSummaries);
            response.setMemberCount(memberSummaries.size());

            // Get role from batch-loaded data (no extra query)
            Role currentUserRole = tripMembers.stream()
                    .filter(m -> m.getUserId().equals(userId))
                    .map(TripMember::getRole)
                    .findFirst()
                    .orElse(null);
            response.setCurrentUserRole(currentUserRole);

            return response;
        });
    }

    /**
     * Updates a trip (owner only for basic info).
     *
     * @contract
     *   - pre: tripId != null, request != null, userId != null
     *   - pre: user must be OWNER
     *   - post: Trip is updated
     *   - calledBy: TripApiController#updateTrip
     *
     * @param tripId The trip ID
     * @param request The update request
     * @param userId The requesting user ID
     * @return The updated trip response
     * @throws ResourceNotFoundException if trip not found
     * @throws ForbiddenException if user is not owner
     * @throws ValidationException if validation fails
     */
    @Transactional
    public TripResponse updateTrip(UUID tripId, UpdateTripRequest request, UUID userId) {
        Trip trip = findTripById(tripId);

        if (!permissionChecker.canDelete(tripId, userId)) { // Only owner can update basic info
            throw new ForbiddenException("只有行程建立者可以修改基本資訊");
        }

        // Validate dates if either is being updated
        LocalDate newStartDate = request.getStartDate() != null ? request.getStartDate() : trip.getStartDate();
        LocalDate newEndDate = request.getEndDate() != null ? request.getEndDate() : trip.getEndDate();
        validateTripDates(newStartDate, newEndDate);

        // Update fields
        if (request.getTitle() != null) {
            trip.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            trip.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            trip.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            trip.setEndDate(request.getEndDate());
        }
        if (request.getBaseCurrency() != null) {
            trip.setBaseCurrency(request.getBaseCurrency());
        }
        if (request.getCoverImageUrl() != null) {
            trip.setCoverImageUrl(request.getCoverImageUrl());
        }

        trip = tripRepository.save(trip);
        log.info("Updated trip: {} by user: {}", tripId, userId);

        TripResponse response = TripResponse.fromEntity(trip);
        response.setMemberCount((int) tripMemberRepository.countByTripId(tripId));
        response.setCurrentUserRole(Role.OWNER);

        return response;
    }

    /**
     * Deletes a trip (owner only).
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user must be OWNER
     *   - post: Trip and all related entities are deleted
     *   - calledBy: TripApiController#deleteTrip
     *
     * @param tripId The trip ID
     * @param userId The requesting user ID
     * @throws ResourceNotFoundException if trip not found
     * @throws ForbiddenException if user is not owner
     */
    @Transactional
    public void deleteTrip(UUID tripId, UUID userId) {
        Trip trip = findTripById(tripId);

        if (!permissionChecker.canDelete(tripId, userId)) {
            throw new ForbiddenException("只有行程建立者可以刪除行程");
        }

        // Delete related entities (in proper order for FK constraints)
        // 1. ExpenseSplits first (depends on Expenses)
        expenseSplitRepository.deleteByTripId(tripId);

        // 2. Expenses (depends on Trip)
        expenseRepository.deleteByTripId(tripId);

        // 3. Documents: clean storage files first, then delete DB records
        documentRepository.findByTripIdOrderByCreatedAtDesc(tripId).forEach(doc -> {
            try {
                String storagePath = tripId + "/" + doc.getFileName();
                storageClient.deleteFile(supabaseProperties.getStorageBucket(), storagePath);
            } catch (Exception e) {
                log.warn("Failed to delete storage file for document {}: {}", doc.getId(), e.getMessage());
            }
        });
        documentRepository.deleteByTripId(tripId);

        // 4. Activities (depends on Trip)
        activityRepository.deleteByTripId(tripId);

        // 5. Todos (depends on Trip)
        todoRepository.deleteByTripId(tripId);

        // 6. InviteLinks (depends on Trip)
        inviteLinkRepository.deleteByTripId(tripId);

        // 7. TripMembers (depends on Trip)
        tripMemberRepository.deleteByTripId(tripId);

        // 8. Delete cover image from storage
        if (trip.getCoverImageUrl() != null) {
            try {
                deleteCoverImage(trip.getCoverImageUrl());
            } catch (Exception e) {
                log.warn("Failed to delete cover image for trip {}: {}", tripId, e.getMessage());
            }
        }

        // 9. Finally delete the Trip itself
        tripRepository.delete(trip);

        log.info("Deleted trip and all related data: {} by user: {}", tripId, userId);
    }

    /**
     * Gets members of a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - post: Returns list of members if user can view
     *   - calledBy: TripApiController#getTripMembers
     *
     * @param tripId The trip ID
     * @param userId The requesting user ID
     * @return List of member summaries
     * @throws ForbiddenException if user cannot view
     */
    @Transactional(readOnly = true)
    public List<TripResponse.MemberSummary> getTripMembers(UUID tripId, UUID userId) {
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程");
        }

        return getMemberSummaries(tripId);
    }

    /**
     * Changes a member's role (owner only).
     *
     * @contract
     *   - pre: tripId != null, targetUserId != null, newRole != null, requesterId != null
     *   - pre: requester must be OWNER
     *   - pre: newRole != OWNER (use transferOwnership instead)
     *   - post: Member's role is updated
     *   - calledBy: TripApiController#changeMemberRole
     *
     * @param tripId The trip ID
     * @param targetUserId The target member's user ID
     * @param newRole The new role
     * @param requesterId The requesting user ID
     * @throws ForbiddenException if requester is not owner
     * @throws ValidationException if trying to assign OWNER role
     */
    @Transactional
    public void changeMemberRole(UUID tripId, UUID targetUserId, Role newRole, UUID requesterId) {
        if (!permissionChecker.canManageMembers(tripId, requesterId)) {
            throw new ForbiddenException("只有行程建立者可以變更成員角色");
        }

        if (newRole == Role.OWNER) {
            throw new ValidationException("INVALID_ROLE_CHANGE", "無法將角色變更為 OWNER，請使用轉移所有權功能");
        }

        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, targetUserId)
                .orElseThrow(() -> ResourceNotFoundException.withCode("MEMBER_NOT_FOUND", "找不到此成員"));

        if (member.getRole() == Role.OWNER) {
            throw new ValidationException("INVALID_ROLE_CHANGE", "無法變更行程建立者的角色");
        }

        member.setRole(newRole);
        tripMemberRepository.save(member);

        log.info("Changed role of user: {} to: {} in trip: {} by: {}",
                targetUserId, newRole, tripId, requesterId);
    }

    /**
     * Removes a member from a trip (owner only).
     *
     * @contract
     *   - pre: tripId != null, targetUserId != null, requesterId != null
     *   - pre: requester must be OWNER
     *   - pre: target cannot be OWNER
     *   - post: Member is removed
     *   - calledBy: TripApiController#removeMember
     *
     * @param tripId The trip ID
     * @param targetUserId The target member's user ID
     * @param requesterId The requesting user ID
     * @throws ForbiddenException if requester is not owner or target is owner
     */
    @Transactional
    public void removeMember(UUID tripId, UUID targetUserId, UUID requesterId) {
        TripMember member = tripMemberRepository.findByTripIdAndUserId(tripId, targetUserId)
                .orElseThrow(() -> ResourceNotFoundException.withCode("MEMBER_NOT_FOUND", "找不到此成員"));

        if (member.getRole() == Role.OWNER) {
            throw new ForbiddenException("無法移除行程建立者");
        }

        if (targetUserId.equals(requesterId)) {
            // Self-removal (leaving the trip) - any non-owner can leave
            tripMemberRepository.delete(member);
            log.info("User {} left trip {}", targetUserId, tripId);
        } else {
            // Removing another member - requires manage permission
            if (!permissionChecker.canManageMembers(tripId, requesterId)) {
                throw new ForbiddenException("只有行程建立者可以移除成員");
            }
            tripMemberRepository.delete(member);
            log.info("Removed user: {} from trip: {} by: {}", targetUserId, tripId, requesterId);
        }
    }

    /**
     * Adds a member to a trip with specified role.
     *
     * @contract
     *   - pre: tripId != null, userId != null, role != null
     *   - pre: role != OWNER
     *   - pre: trip must exist and not be at member limit
     *   - post: New member is added
     *
     * @param tripId The trip ID
     * @param userId The user ID to add
     * @param role The role to assign
     * @throws ValidationException if already a member or limit exceeded
     */
    @Transactional
    public void addMember(UUID tripId, UUID userId, Role role) {
        findTripById(tripId); // Verify trip exists

        if (tripMemberRepository.existsByTripIdAndUserId(tripId, userId)) {
            throw new ValidationException("DUPLICATE_MEMBER", "已是行程成員");
        }

        long currentCount = tripMemberRepository.countByTripId(tripId);
        if (currentCount >= MAX_MEMBERS_PER_TRIP) {
            throw new ValidationException("MEMBER_LIMIT_EXCEEDED", "行程成員已達上限（" + MAX_MEMBERS_PER_TRIP + "人）");
        }

        TripMember member = TripMember.builder()
                .tripId(tripId)
                .userId(userId)
                .role(role)
                .build();

        tripMemberRepository.save(member);

        log.info("Added user: {} to trip: {} with role: {}", userId, tripId, role);
    }

    /**
     * Gets the member count for a trip.
     *
     * @param tripId The trip ID
     * @return The number of members
     */
    public int getMemberCount(UUID tripId) {
        return (int) tripMemberRepository.countByTripId(tripId);
    }

    // Private helper methods

    private Trip findTripById(UUID tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> ResourceNotFoundException.withCode("TRIP_NOT_FOUND", "行程不存在"));
    }

    private void validateTripDates(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("INVALID_DATE_RANGE", "結束日期不可早於開始日期");
        }
    }

    private static final String UNKNOWN_USER_NAME = "Unknown";

    private List<TripResponse.MemberSummary> getMemberSummaries(UUID tripId) {
        List<TripMember> members = tripMemberRepository.findByTripId(tripId);

        List<UUID> userIds = members.stream()
                .map(TripMember::getUserId)
                .toList();
        Map<UUID, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, java.util.function.Function.identity()));

        return members.stream()
                .map(member -> {
                    User user = userMap.get(member.getUserId());
                    return TripResponse.MemberSummary.builder()
                            .userId(member.getUserId())
                            .nickname(user != null ? user.getNickname() : UNKNOWN_USER_NAME)
                            .avatarUrl(user != null ? user.getAvatarUrl() : null)
                            .role(member.getRole())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ===== Cover Image Upload Methods =====

    /**
     * Uploads a cover image for a trip.
     *
     * @contract
     *   - pre: file != null, file.size <= 5MB
     *   - pre: file type is JPEG, PNG, or WebP
     *   - pre: file content matches declared MIME type (magic bytes validated)
     *   - pre: tripId != null (trip must exist first to prevent orphaned files)
     *   - pre: user has OWNER or EDITOR permission on the trip
     *   - post: Image uploaded to storage at covers/{tripId}/{uuid}.{ext}
     *   - post: Returns public URL of uploaded image
     *   - calledBy: TripController#createTrip, TripController#updateTrip
     *
     * @param tripId The trip ID (required - trip must exist first)
     * @param userId The user performing the upload
     * @param file The cover image file
     * @return The public URL of the uploaded image
     * @throws ValidationException if file validation fails
     * @throws ForbiddenException if user lacks permission
     * @throws IllegalArgumentException if tripId is null
     */
    @Transactional
    public String uploadCoverImage(UUID tripId, UUID userId, MultipartFile file) {
        // tripId is required to prevent orphaned files
        if (tripId == null) {
            throw new IllegalArgumentException("tripId is required - create trip first before uploading cover image");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is required for cover image upload");
        }

        log.debug("Uploading cover image for trip {} by user {}", tripId, userId);

        // Validate file
        validateCoverImage(file);

        // Permission check
        if (!permissionChecker.canEdit(tripId, userId)) {
            throw new ForbiddenException("您沒有權限修改此行程");
        }

        // Generate storage path: covers/{tripId}/{uuid}.{ext}
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + (fileExtension.isEmpty() ? ".jpg" : "." + fileExtension);
        String storagePath = "covers/" + tripId + "/" + storedFileName;

        try {
            byte[] content = file.getBytes();
            String fileUrl = storageClient.uploadFile(
                    supabaseProperties.getCoverImageBucket(),
                    storagePath,
                    content,
                    file.getContentType()
            );

            log.info("Uploaded cover image to {}", storagePath);
            return fileUrl;
        } catch (IOException e) {
            log.error("Failed to read cover image content", e);
            throw new ValidationException("FILE_READ_ERROR", "無法讀取圖片內容");
        }
    }

    /**
     * Deletes a cover image from storage.
     *
     * @contract
     *   - pre: coverImageUrl != null
     *   - post: File deleted from storage (silently ignores if not found)
     *   - calledBy: TripController#updateTrip (when replacing cover)
     *
     * @param coverImageUrl The URL of the cover image to delete
     */
    public void deleteCoverImage(String coverImageUrl) {
        if (coverImageUrl == null || coverImageUrl.isEmpty()) {
            return;
        }

        try {
            // Extract path from URL
            String path = extractStoragePath(coverImageUrl);
            if (path != null) {
                storageClient.deleteFile(supabaseProperties.getCoverImageBucket(), path);
                log.info("Deleted old cover image: {}", path);
            }
        } catch (Exception e) {
            log.warn("Failed to delete old cover image: {}", coverImageUrl, e);
            // Don't throw - old image cleanup failure shouldn't block operation
        }
    }

    private void validateCoverImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("EMPTY_FILE", "請選擇要上傳的封面圖片");
        }

        if (file.getSize() > MAX_COVER_IMAGE_SIZE) {
            throw new ValidationException("FILE_TOO_LARGE",
                    "封面圖片大小超過限制 (最大 5 MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new ValidationException("UNSUPPORTED_FORMAT",
                    "不支援的圖片格式。支援格式：JPEG, PNG, WebP");
        }

        if (!validateImageMagicBytes(file, contentType)) {
            log.warn("Image content does not match declared MIME type: {}", contentType);
            throw new ValidationException("INVALID_FILE_CONTENT",
                    "圖片內容與宣告的格式不符");
        }
    }

    private boolean validateImageMagicBytes(MultipartFile file, String declaredType) {
        byte[][] expectedSignatures = IMAGE_MAGIC_BYTES.get(declaredType);
        if (expectedSignatures == null) {
            return true; // No validation defined
        }

        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12]; // WebP needs 12 bytes for full validation
            int bytesRead = is.read(header);

            if (bytesRead < 4) {
                return false;
            }

            for (byte[] signature : expectedSignatures) {
                if (bytesRead >= signature.length) {
                    boolean matches = true;
                    for (int i = 0; i < signature.length; i++) {
                        if (header[i] != signature[i]) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
                        // Additional WebP validation: check WEBP signature at offset 8
                        if ("image/webp".equals(declaredType)) {
                            return bytesRead >= 12 &&
                                    header[8] == 'W' && header[9] == 'E' &&
                                    header[10] == 'B' && header[11] == 'P';
                        }
                        return true;
                    }
                }
            }
            return false;
        } catch (IOException e) {
            log.error("Failed to read file for magic bytes validation", e);
            return false;
        }
    }

    private String extractStoragePath(String url) {
        // Extract path after /public/bucket/
        // Use coverImageBucket for cover images, not storageBucket (documents)
        String marker = "/public/" + supabaseProperties.getCoverImageBucket() + "/";
        int index = url.indexOf(marker);
        if (index >= 0) {
            String path = url.substring(index + marker.length());

            // SECURITY: Prevent path traversal attacks
            if (path.contains("..") || path.contains("//") || path.startsWith("/")) {
                log.warn("Potential path traversal detected in URL: {}", url);
                return null;
            }

            // Validate path starts with expected prefix (covers/)
            if (!path.startsWith("covers/")) {
                log.warn("Path does not start with expected prefix: {}", path);
                return null;
            }

            return path;
        }
        return null;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        // Only allow known safe extensions to prevent executable file uploads
        return ALLOWED_EXTENSIONS.contains(extension) ? extension : "";
    }
}
