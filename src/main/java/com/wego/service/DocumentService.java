package com.wego.service;

import com.wego.config.SupabaseProperties;
import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreateDocumentRequest;
import com.wego.dto.response.DocumentResponse;
import com.wego.entity.Document;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.ValidationException;
import com.wego.repository.DocumentRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import com.wego.service.external.StorageClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for document management operations.
 *
 * @contract
 *   - All methods validate permissions before executing
 *   - File size and type validations are enforced
 *   - Storage operations use Supabase Storage via StorageClient
 *
 * @see Document
 * @see StorageClient
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    /**
     * Allowed MIME types for document upload.
     */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/heic"
    );

    /**
     * Magic bytes for file type validation.
     * Used to verify actual file content, not just Content-Type header.
     */
    private static final Map<String, byte[][]> MAGIC_BYTES = Map.of(
            "application/pdf", new byte[][] { {0x25, 0x50, 0x44, 0x46} }, // %PDF
            "image/jpeg", new byte[][] {
                {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0},
                {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE1},
                {(byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE8}
            },
            "image/png", new byte[][] { {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A} }
    );

    private final DocumentRepository documentRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final StorageClient storageClient;
    private final PermissionChecker permissionChecker;
    private final SupabaseProperties supabaseProperties;

    /**
     * Uploads a document to a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null, file != null
     *   - pre: user has edit permission on trip
     *   - pre: file size <= 10 MB
     *   - pre: trip storage <= 100 MB after upload
     *   - pre: file type is supported (PDF, JPEG, PNG)
     *   - post: Document is stored and metadata persisted
     *   - calledBy: DocumentApiController#uploadDocument
     *
     * @param tripId The trip ID
     * @param userId The uploading user's ID
     * @param file The multipart file to upload
     * @param request Additional document metadata
     * @return The created document response
     * @throws ForbiddenException if user has no edit permission
     * @throws ValidationException if file validation fails
     * @throws ResourceNotFoundException if trip not found
     */
    @Transactional
    public DocumentResponse uploadDocument(UUID tripId, UUID userId, MultipartFile file, CreateDocumentRequest request) {
        log.debug("Uploading document to trip {} by user {}", tripId, userId);

        // Check permission
        if (!permissionChecker.canEdit(tripId, userId)) {
            throw new ForbiddenException("您沒有權限上傳檔案到此行程");
        }

        // Verify trip exists
        if (!tripRepository.existsById(tripId)) {
            throw ResourceNotFoundException.withCode("TRIP_NOT_FOUND", "行程不存在");
        }

        // Validate file
        validateFile(file, tripId);

        // Generate unique file name
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String storedFileName = UUID.randomUUID() + (fileExtension.isEmpty() ? "" : "." + fileExtension);
        String storagePath = tripId + "/" + storedFileName;

        try {
            // Upload to storage
            byte[] content = file.getBytes();
            String fileUrl = storageClient.uploadFile(
                    supabaseProperties.getStorageBucket(),
                    storagePath,
                    content,
                    file.getContentType()
            );

            // Create document entity
            Document document = Document.builder()
                    .tripId(tripId)
                    .fileName(storedFileName)
                    .originalFileName(originalFileName)
                    .fileUrl(fileUrl)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .relatedActivityId(request != null ? request.getRelatedActivityId() : null)
                    .relatedDay(request != null ? request.getRelatedDay() : null)
                    .description(request != null ? request.getDescription() : null)
                    .uploadedBy(userId)
                    .build();

            document = documentRepository.save(document);
            log.info("Uploaded document {} to trip {} by user {}", document.getId(), tripId, userId);

            return buildDocumentResponse(document);
        } catch (IOException e) {
            log.error("Failed to read file content", e);
            throw new ValidationException("FILE_READ_ERROR", "無法讀取檔案內容");
        }
    }

    /**
     * Gets all documents for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: returns list of documents ordered by creation date desc
     *   - calledBy: DocumentApiController#getDocumentsByTrip
     *
     * @param tripId The trip ID
     * @param userId The requesting user's ID
     * @return List of document responses
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByTrip(UUID tripId, UUID userId) {
        log.debug("Getting documents for trip {} by user {}", tripId, userId);

        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程的檔案");
        }

        List<Document> documents = documentRepository.findByTripIdOrderByCreatedAtDesc(tripId);

        return documents.stream()
                .map(this::buildDocumentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gets a single document by ID.
     *
     * @contract
     *   - pre: tripId != null, documentId != null, userId != null
     *   - pre: document must belong to the specified trip
     *   - pre: user has view permission on the document's trip
     *   - post: returns document details
     *   - calledBy: DocumentApiController#getDocument
     *
     * @param tripId The expected trip ID (for security validation)
     * @param documentId The document ID
     * @param userId The requesting user's ID
     * @return The document response
     * @throws ResourceNotFoundException if document not found or doesn't belong to trip
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public DocumentResponse getDocument(UUID tripId, UUID documentId, UUID userId) {
        log.debug("Getting document {} from trip {} by user {}", documentId, tripId, userId);

        Document document = findDocumentById(documentId);

        // Security: Verify document belongs to the specified trip
        if (!document.getTripId().equals(tripId)) {
            throw ResourceNotFoundException.withCode("DOCUMENT_NOT_FOUND", "檔案不存在");
        }

        if (!permissionChecker.canView(document.getTripId(), userId)) {
            throw new ForbiddenException("您沒有權限查看此檔案");
        }

        return buildDocumentResponse(document);
    }

    /**
     * Generates a signed download URL for a document.
     *
     * @contract
     *   - pre: tripId != null, documentId != null, userId != null
     *   - pre: document must belong to the specified trip
     *   - pre: user has view permission on the document's trip
     *   - post: returns signed URL valid for configured expiry time
     *   - calledBy: DocumentApiController#getDownloadUrl
     *
     * @param tripId The expected trip ID (for security validation)
     * @param documentId The document ID
     * @param userId The requesting user's ID
     * @return The signed download URL
     * @throws ResourceNotFoundException if document not found or doesn't belong to trip
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID tripId, UUID documentId, UUID userId) {
        log.debug("Getting download URL for document {} from trip {} by user {}", documentId, tripId, userId);

        Document document = findDocumentById(documentId);

        // Security: Verify document belongs to the specified trip
        if (!document.getTripId().equals(tripId)) {
            throw ResourceNotFoundException.withCode("DOCUMENT_NOT_FOUND", "檔案不存在");
        }

        if (!permissionChecker.canView(document.getTripId(), userId)) {
            throw new ForbiddenException("您沒有權限下載此檔案");
        }

        String storagePath = document.getTripId() + "/" + document.getFileName();
        return storageClient.getSignedUrl(
                supabaseProperties.getStorageBucket(),
                storagePath,
                supabaseProperties.getSignedUrlExpiry()
        );
    }

    /**
     * Deletes a document.
     *
     * @contract
     *   - pre: tripId != null, documentId != null, userId != null
     *   - pre: document must belong to the specified trip
     *   - pre: user is the uploader OR user is trip OWNER
     *   - post: document is deleted from storage and database
     *   - calledBy: DocumentApiController#deleteDocument
     *
     * @param tripId The expected trip ID (for security validation)
     * @param documentId The document ID
     * @param userId The requesting user's ID
     * @throws ResourceNotFoundException if document not found or doesn't belong to trip
     * @throws ForbiddenException if user has no delete permission
     */
    @Transactional
    public void deleteDocument(UUID tripId, UUID documentId, UUID userId) {
        log.debug("Deleting document {} from trip {} by user {}", documentId, tripId, userId);

        Document document = findDocumentById(documentId);

        // Security: Verify document belongs to the specified trip
        if (!document.getTripId().equals(tripId)) {
            throw ResourceNotFoundException.withCode("DOCUMENT_NOT_FOUND", "檔案不存在");
        }

        // Check permission: uploader can delete their own, owner can delete any
        if (!permissionChecker.canView(document.getTripId(), userId)) {
            throw new ForbiddenException("您沒有權限存取此檔案");
        }

        boolean isUploader = document.getUploadedBy().equals(userId);
        boolean isOwner = permissionChecker.canDelete(document.getTripId(), userId);

        if (!isUploader && !isOwner) {
            throw new ForbiddenException("您沒有權限刪除此檔案");
        }

        // Delete from storage
        String storagePath = document.getTripId() + "/" + document.getFileName();
        storageClient.deleteFile(supabaseProperties.getStorageBucket(), storagePath);

        // Delete from database
        documentRepository.delete(document);
        log.info("Deleted document {} from trip {} by user {}", documentId, document.getTripId(), userId);
    }

    /**
     * Gets the total storage usage for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: returns total file size in bytes
     *   - calledBy: DocumentApiController#getTripStorageUsage
     *
     * @param tripId The trip ID
     * @param userId The requesting user's ID
     * @return Total storage usage in bytes
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public long getTripStorageUsage(UUID tripId, UUID userId) {
        log.debug("Getting storage usage for trip {} by user {}", tripId, userId);

        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程");
        }

        return documentRepository.sumFileSizeByTripId(tripId);
    }

    /**
     * Gets documents linked to a specific activity within a specific trip.
     *
     * @contract
     *   - pre: activityId, tripId, userId are non-null
     *   - post: Returns only documents that belong to both the trip and activity
     *   - throws: ForbiddenException if user has no view permission
     *
     * @param activityId The activity ID
     * @param tripId The trip ID (used for permission check AND filtering)
     * @param userId The requesting user's ID
     * @return List of document responses
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByActivity(UUID activityId, UUID tripId, UUID userId) {
        log.debug("Getting documents for activity {} in trip {} by user {}", activityId, tripId, userId);

        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程的檔案");
        }

        // Use tripId AND activityId to prevent IDOR - ensures activity belongs to the authorized trip
        List<Document> documents = documentRepository.findByTripIdAndRelatedActivityId(tripId, activityId);

        return documents.stream()
                .map(this::buildDocumentResponse)
                .collect(Collectors.toList());
    }

    // Private helper methods

    private Document findDocumentById(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> ResourceNotFoundException.withCode("DOCUMENT_NOT_FOUND", "檔案不存在"));
    }

    private void validateFile(MultipartFile file, UUID tripId) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new ValidationException("EMPTY_FILE", "請選擇要上傳的檔案");
        }

        // Check file size (max 10 MB)
        if (file.getSize() > Document.MAX_FILE_SIZE) {
            throw new ValidationException("FILE_TOO_LARGE",
                    "檔案大小超過限制 (最大 " + (Document.MAX_FILE_SIZE / 1024 / 1024) + " MB)");
        }

        // Check MIME type from header
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new ValidationException("UNSUPPORTED_FORMAT",
                    "不支援的檔案格式。支援格式：PDF, JPEG, PNG");
        }

        // CRITICAL: Validate actual file content using magic bytes
        // This prevents MIME type spoofing attacks
        if (!validateFileMagicBytes(file, contentType)) {
            log.warn("File content does not match declared MIME type: {}", contentType);
            throw new ValidationException("INVALID_FILE_CONTENT",
                    "檔案內容與宣告的格式不符");
        }

        // Check trip storage limit (max 100 MB)
        long currentUsage = documentRepository.sumFileSizeByTripId(tripId);
        if (currentUsage + file.getSize() > Document.MAX_TRIP_STORAGE) {
            throw new ValidationException("STORAGE_LIMIT_EXCEEDED",
                    "行程儲存空間已滿 (最大 " + (Document.MAX_TRIP_STORAGE / 1024 / 1024) + " MB)");
        }
    }

    /**
     * Validates file content using magic bytes (file signature).
     * Prevents MIME type spoofing by checking actual file content.
     *
     * @param file The multipart file to validate
     * @param declaredType The declared MIME type
     * @return true if file content matches declared type
     */
    private boolean validateFileMagicBytes(MultipartFile file, String declaredType) {
        // Skip validation for HEIC (complex format, allow if declared)
        if ("image/heic".equals(declaredType)) {
            return true;
        }

        byte[][] expectedSignatures = MAGIC_BYTES.get(declaredType);
        if (expectedSignatures == null) {
            // No magic bytes defined for this type, skip validation
            return true;
        }

        try (InputStream is = file.getInputStream()) {
            // Read enough bytes to check longest signature
            int maxLength = Arrays.stream(expectedSignatures)
                    .mapToInt(sig -> sig.length)
                    .max()
                    .orElse(8);
            byte[] fileHeader = new byte[maxLength];
            int bytesRead = is.read(fileHeader);

            if (bytesRead < 4) {
                return false; // File too small to validate
            }

            // Check if file starts with any expected signature
            for (byte[] signature : expectedSignatures) {
                if (bytesRead >= signature.length) {
                    boolean matches = true;
                    for (int i = 0; i < signature.length; i++) {
                        if (fileHeader[i] != signature[i]) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
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

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private DocumentResponse buildDocumentResponse(Document document) {
        DocumentResponse response = DocumentResponse.fromEntity(document);

        // Add uploader info
        userRepository.findById(document.getUploadedBy()).ifPresent(user -> {
            response.setUploadedByName(user.getNickname());
            response.setUploadedByAvatarUrl(user.getAvatarUrl());
        });

        return response;
    }

    /**
     * Gets document count for a trip.
     *
     * @contract
     *   - pre: tripId != null, userId != null
     *   - pre: user has view permission on trip
     *   - post: returns count of documents in trip
     *   - calledBy: TripController#showTripDetail
     *
     * @param tripId The trip ID
     * @param userId The ID of the user requesting
     * @return Number of documents in trip
     * @throws ForbiddenException if user has no view permission
     */
    @Transactional(readOnly = true)
    public long getDocumentCount(UUID tripId, UUID userId) {
        if (!permissionChecker.canView(tripId, userId)) {
            throw new ForbiddenException("您沒有權限查看此行程");
        }
        return documentRepository.countByTripId(tripId);
    }
}
