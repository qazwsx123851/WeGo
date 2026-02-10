package com.wego.controller.api;

import com.wego.config.SupabaseProperties;
import com.wego.dto.ApiResponse;
import com.wego.dto.request.CreateDocumentRequest;
import com.wego.dto.response.DocumentResponse;
import com.wego.entity.Document;
import com.wego.security.CurrentUser;
import com.wego.security.UserPrincipal;
import com.wego.service.DocumentService;
import com.wego.service.external.StorageClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final StorageClient storageClient;
    private final SupabaseProperties supabaseProperties;

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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("GET /api/trips/{}/documents/{}/download by user {}", tripId, documentId, userId);

        String downloadUrl = documentService.getDownloadUrl(tripId, documentId, userId);

        Map<String, String> response = new HashMap<>();
        response.put("downloadUrl", downloadUrl);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Serves document content for inline preview (PDF/image).
     *
     * @contract
     *   - pre: user is authenticated and has view permission
     *   - post: returns raw file bytes with correct Content-Type
     *   - post: sets security headers (nosniff, SAMEORIGIN, sandbox CSP)
     *   - post: sets cache headers (private, 1 hour, immutable)
     *   - calls: DocumentService#getDocumentForPreview, StorageClient#downloadFile
     *
     * GET /api/trips/{tripId}/documents/{documentId}/preview
     */
    @GetMapping("/trips/{tripId}/documents/{documentId}/preview")
    public ResponseEntity<byte[]> previewDocument(
            @PathVariable UUID tripId,
            @PathVariable UUID documentId,
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("GET /api/trips/{}/documents/{}/preview by user {}", tripId, documentId, userId);

        Document document = documentService.getDocumentForPreview(tripId, documentId, userId);

        String storagePath = document.getTripId() + "/" + document.getFileName();
        byte[] content = storageClient.downloadFile(
                supabaseProperties.getStorageBucket(), storagePath);

        if (content == null || content.length == 0) {
            log.warn("File content is empty for preview: {}", storagePath);
            return ResponseEntity.notFound().build();
        }

        // RFC 5987 Content-Disposition with UTF-8 filename support
        String originalFileName = document.getOriginalFileName();
        String encodedFileName = URLEncoder.encode(
                originalFileName, StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = "inline; filename=\"" + encodedFileName
                + "\"; filename*=UTF-8''" + encodedFileName;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, document.getMimeType())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .header("X-Frame-Options", "SAMEORIGIN")
                .header("Content-Security-Policy",
                        "sandbox; default-src 'none'; style-src 'unsafe-inline'")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600, immutable")
                .body(content);
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
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
            @CurrentUser UserPrincipal principal) {

        UUID userId = principal.getId();
        log.debug("GET /api/trips/{}/activities/{}/documents by user {}", tripId, activityId, userId);

        List<DocumentResponse> documents = documentService.getDocumentsByActivity(activityId, tripId, userId);

        return ResponseEntity.ok(ApiResponse.success(documents));
    }

}
