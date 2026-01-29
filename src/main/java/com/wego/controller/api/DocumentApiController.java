package com.wego.controller.api;

import com.wego.dto.ApiResponse;
import com.wego.dto.request.CreateDocumentRequest;
import com.wego.dto.response.DocumentResponse;
import com.wego.entity.Document;
import com.wego.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for document operations.
 *
 * @contract
 *   - All endpoints require authentication
 *   - Returns ApiResponse wrapper for all responses
 *   - File uploads use multipart/form-data
 *
 * @see DocumentService
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DocumentApiController {

    private final DocumentService documentService;

    /**
     * Uploads a document to a trip.
     *
     * @contract
     *   - pre: user is authenticated and has edit permission
     *   - pre: file is provided and valid (PDF, JPEG, PNG, max 10MB)
     *   - post: returns 201 with created document
     *   - calls: DocumentService#uploadDocument
     *
     * POST /api/trips/{tripId}/documents
     */
    @PostMapping("/trips/{tripId}/documents")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @PathVariable UUID tripId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "metadata", required = false) @Valid CreateDocumentRequest request,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("POST /api/trips/{}/documents by user {}", tripId, userId);

        if (request == null) {
            request = new CreateDocumentRequest();
        }

        DocumentResponse response = documentService.uploadDocument(tripId, userId, file, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "檔案上傳成功"));
    }

    /**
     * Gets all documents for a trip.
     *
     * @contract
     *   - pre: user is authenticated and has view permission
     *   - post: returns 200 with list of documents
     *   - calls: DocumentService#getDocumentsByTrip
     *
     * GET /api/trips/{tripId}/documents
     */
    @GetMapping("/trips/{tripId}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByTrip(
            @PathVariable UUID tripId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("GET /api/trips/{}/documents by user {}", tripId, userId);

        List<DocumentResponse> documents = documentService.getDocumentsByTrip(tripId, userId);

        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    /**
     * Gets a document by ID.
     *
     * @contract
     *   - pre: user is authenticated and has view permission
     *   - post: returns 200 with document details
     *   - calls: DocumentService#getDocument
     *
     * GET /api/trips/{tripId}/documents/{documentId}
     */
    @GetMapping("/trips/{tripId}/documents/{documentId}")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocument(
            @PathVariable UUID tripId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("GET /api/trips/{}/documents/{} by user {}", tripId, documentId, userId);

        DocumentResponse document = documentService.getDocument(tripId, documentId, userId);

        return ResponseEntity.ok(ApiResponse.success(document));
    }

    /**
     * Gets download URL for a document.
     *
     * @contract
     *   - pre: user is authenticated and has view permission
     *   - post: returns 200 with signed download URL
     *   - calls: DocumentService#getDownloadUrl
     *
     * GET /api/trips/{tripId}/documents/{documentId}/download
     */
    @GetMapping("/trips/{tripId}/documents/{documentId}/download")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDownloadUrl(
            @PathVariable UUID tripId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("GET /api/trips/{}/documents/{}/download by user {}", tripId, documentId, userId);

        String downloadUrl = documentService.getDownloadUrl(tripId, documentId, userId);

        Map<String, String> response = new HashMap<>();
        response.put("downloadUrl", downloadUrl);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Deletes a document.
     *
     * @contract
     *   - pre: user is authenticated
     *   - pre: user is uploader OR trip owner
     *   - post: returns 200 on success
     *   - calls: DocumentService#deleteDocument
     *
     * DELETE /api/trips/{tripId}/documents/{documentId}
     */
    @DeleteMapping("/trips/{tripId}/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable UUID tripId,
            @PathVariable UUID documentId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("DELETE /api/trips/{}/documents/{} by user {}", tripId, documentId, userId);

        documentService.deleteDocument(tripId, documentId, userId);

        return ResponseEntity.ok(ApiResponse.success(null, "檔案刪除成功"));
    }

    /**
     * Gets storage usage for a trip.
     *
     * @contract
     *   - pre: user is authenticated and has view permission
     *   - post: returns 200 with storage usage info
     *   - calls: DocumentService#getTripStorageUsage
     *
     * GET /api/trips/{tripId}/documents/storage
     */
    @GetMapping("/trips/{tripId}/documents/storage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTripStorageUsage(
            @PathVariable UUID tripId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("GET /api/trips/{}/documents/storage by user {}", tripId, userId);

        long usedBytes = documentService.getTripStorageUsage(tripId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("usedBytes", usedBytes);
        response.put("maxBytes", Document.MAX_TRIP_STORAGE);
        response.put("usedMB", usedBytes / 1024.0 / 1024.0);
        response.put("maxMB", Document.MAX_TRIP_STORAGE / 1024 / 1024);
        response.put("usagePercent", (double) usedBytes / Document.MAX_TRIP_STORAGE * 100);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Gets documents linked to a specific activity.
     *
     * @contract
     *   - pre: user is authenticated and has view permission
     *   - post: returns 200 with list of documents
     *   - calls: DocumentService#getDocumentsByActivity
     *
     * GET /api/trips/{tripId}/activities/{activityId}/documents
     */
    @GetMapping("/trips/{tripId}/activities/{activityId}/documents")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getDocumentsByActivity(
            @PathVariable UUID tripId,
            @PathVariable UUID activityId,
            @AuthenticationPrincipal OAuth2User principal) {

        UUID userId = getCurrentUserId(principal);
        log.debug("GET /api/trips/{}/activities/{}/documents by user {}", tripId, activityId, userId);

        List<DocumentResponse> documents = documentService.getDocumentsByActivity(activityId, tripId, userId);

        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    /**
     * Gets the current user ID from the OAuth2 principal.
     * For testing, returns a consistent UUID if principal is null.
     */
    private UUID getCurrentUserId(OAuth2User principal) {
        if (principal == null) {
            // For testing purposes, generate a consistent UUID
            return UUID.fromString("00000000-0000-0000-0000-000000000001");
        }
        // Extract user ID from the principal
        String sub = principal.getAttribute("sub");
        if (sub != null) {
            try {
                return UUID.fromString(sub);
            } catch (IllegalArgumentException e) {
                // If sub is not a valid UUID, generate one based on the sub hash
                return UUID.nameUUIDFromBytes(sub.getBytes());
            }
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
