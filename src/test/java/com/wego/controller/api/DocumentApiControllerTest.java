package com.wego.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.dto.request.CreateDocumentRequest;
import com.wego.dto.response.DocumentResponse;
import com.wego.entity.Document;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.ValidationException;
import com.wego.security.UserPrincipal;
import com.wego.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for DocumentApiController.
 *
 * @contract
 *   - Tests all API endpoints
 *   - Verifies request validation
 *   - Tests error handling
 */
@WebMvcTest(DocumentApiController.class)
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
class DocumentApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    private UUID tripId;
    private UUID documentId;
    private UUID userId;
    private UUID activityId;
    private User testUser;
    private UserPrincipal userPrincipal;
    private DocumentResponse testDocumentResponse;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        userId = UUID.randomUUID();
        activityId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .provider("google")
                .providerId("google-123")
                .build();

        // Create UserPrincipal with attributes containing "sub" for authentication
        Map<String, Object> attributes = Map.of("sub", userId.toString());
        userPrincipal = new UserPrincipal(testUser, attributes);

        testDocumentResponse = DocumentResponse.builder()
                .id(documentId)
                .tripId(tripId)
                .fileName("test-uuid.pdf")
                .originalFileName("receipt.pdf")
                .fileUrl("https://storage.example.com/documents/test-uuid.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .description("Test document")
                .uploadedBy(userId)
                .uploadedByName("Test User")
                .createdAt(Instant.now())
                .isPdf(true)
                .isImage(false)
                .fileExtension("pdf")
                .build();
    }

    @Nested
    @DisplayName("POST /api/trips/{tripId}/documents")
    class UploadDocumentTests {

        @Test
        @DisplayName("should upload document and return 201")
        void uploadDocument_withValidFile_shouldReturn201() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "receipt.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );

            when(documentService.uploadDocument(eq(tripId), any(UUID.class), any(), any()))
                    .thenReturn(testDocumentResponse);

            // When & Then
            mockMvc.perform(multipart("/api/trips/{tripId}/documents", tripId)
                            .file(file)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(documentId.toString()))
                    .andExpect(jsonPath("$.data.originalFileName").value("receipt.pdf"));
        }

        @Test
        @DisplayName("should upload document with metadata and return 201")
        void uploadDocument_withMetadata_shouldReturn201() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "receipt.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );
            CreateDocumentRequest metadata = CreateDocumentRequest.builder()
                    .relatedActivityId(activityId)
                    .relatedDay(1)
                    .description("Lunch receipt")
                    .build();
            MockMultipartFile metadataFile = new MockMultipartFile(
                    "metadata",
                    "",
                    MediaType.APPLICATION_JSON_VALUE,
                    objectMapper.writeValueAsBytes(metadata)
            );

            when(documentService.uploadDocument(eq(tripId), any(UUID.class), any(), any()))
                    .thenReturn(testDocumentResponse);

            // When & Then
            mockMvc.perform(multipart("/api/trips/{tripId}/documents", tripId)
                            .file(file)
                            .file(metadataFile)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("should return 400 when file exceeds size limit")
        void uploadDocument_exceedingSizeLimit_shouldReturn400() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "large.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );

            when(documentService.uploadDocument(eq(tripId), any(UUID.class), any(), any()))
                    .thenThrow(new ValidationException("FILE_TOO_LARGE", "檔案大小超過限制"));

            // When & Then
            mockMvc.perform(multipart("/api/trips/{tripId}/documents", tripId)
                            .file(file)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("FILE_TOO_LARGE"));
        }

        @Test
        @DisplayName("should return 400 when file format is unsupported")
        void uploadDocument_unsupportedFormat_shouldReturn400() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "virus.exe",
                    "application/x-msdownload",
                    "test content".getBytes()
            );

            when(documentService.uploadDocument(eq(tripId), any(UUID.class), any(), any()))
                    .thenThrow(new ValidationException("UNSUPPORTED_FORMAT", "不支援的檔案格式"));

            // When & Then
            mockMvc.perform(multipart("/api/trips/{tripId}/documents", tripId)
                            .file(file)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_FORMAT"));
        }

        @Test
        @DisplayName("should return 403 when user has no permission")
        void uploadDocument_noPermission_shouldReturn403() throws Exception {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "receipt.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );

            when(documentService.uploadDocument(eq(tripId), any(UUID.class), any(), any()))
                    .thenThrow(new ForbiddenException("您沒有權限上傳檔案到此行程"));

            // When & Then
            mockMvc.perform(multipart("/api/trips/{tripId}/documents", tripId)
                            .file(file)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/documents")
    class GetDocumentsByTripTests {

        @Test
        @DisplayName("should return documents list with 200")
        void getDocumentsByTrip_shouldReturn200() throws Exception {
            // Given
            List<DocumentResponse> documents = Arrays.asList(testDocumentResponse);
            when(documentService.getDocumentsByTrip(eq(tripId), any(UUID.class)))
                    .thenReturn(documents);

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/documents", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(documentId.toString()));
        }

        @Test
        @DisplayName("should return empty list when no documents")
        void getDocumentsByTrip_noDocuments_shouldReturnEmptyList() throws Exception {
            // Given
            when(documentService.getDocumentsByTrip(eq(tripId), any(UUID.class)))
                    .thenReturn(Collections.emptyList());

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/documents", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("should return 403 when user has no permission")
        void getDocumentsByTrip_noPermission_shouldReturn403() throws Exception {
            // Given
            when(documentService.getDocumentsByTrip(eq(tripId), any(UUID.class)))
                    .thenThrow(new ForbiddenException("您沒有權限查看此行程的檔案"));

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/documents", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/documents/{documentId}")
    class GetDocumentTests {

        @Test
        @DisplayName("should return document with 200")
        void getDocument_shouldReturn200() throws Exception {
            // Given
            when(documentService.getDocument(eq(tripId), eq(documentId), any(UUID.class)))
                    .thenReturn(testDocumentResponse);

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/documents/{documentId}", tripId, documentId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(documentId.toString()))
                    .andExpect(jsonPath("$.data.originalFileName").value("receipt.pdf"));
        }

        @Test
        @DisplayName("should return 404 when document not found")
        void getDocument_notFound_shouldReturn404() throws Exception {
            // Given
            when(documentService.getDocument(eq(tripId), eq(documentId), any(UUID.class)))
                    .thenThrow(ResourceNotFoundException.withCode("DOCUMENT_NOT_FOUND", "檔案不存在"));

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/documents/{documentId}", tripId, documentId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("DOCUMENT_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/documents/{documentId}/download")
    class GetDownloadUrlTests {

        @Test
        @DisplayName("should return download URL with 200")
        void getDownloadUrl_shouldReturn200() throws Exception {
            // Given
            String signedUrl = "https://storage.example.com/signed/document.pdf?token=xxx";
            when(documentService.getDownloadUrl(eq(tripId), eq(documentId), any(UUID.class)))
                    .thenReturn(signedUrl);

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/documents/{documentId}/download", tripId, documentId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.downloadUrl").value(signedUrl));
        }

        @Test
        @DisplayName("should return 403 when user has no permission")
        void getDownloadUrl_noPermission_shouldReturn403() throws Exception {
            // Given
            when(documentService.getDownloadUrl(eq(tripId), eq(documentId), any(UUID.class)))
                    .thenThrow(new ForbiddenException("您沒有權限下載此檔案"));

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/documents/{documentId}/download", tripId, documentId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("DELETE /api/trips/{tripId}/documents/{documentId}")
    class DeleteDocumentTests {

        @Test
        @DisplayName("should delete document and return 200")
        void deleteDocument_shouldReturn200() throws Exception {
            // Given
            doNothing().when(documentService).deleteDocument(eq(tripId), eq(documentId), any(UUID.class));

            // When & Then
            mockMvc.perform(delete("/api/trips/{tripId}/documents/{documentId}", tripId, documentId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("檔案刪除成功"));
        }

        @Test
        @DisplayName("should return 404 when document not found")
        void deleteDocument_notFound_shouldReturn404() throws Exception {
            // Given
            doThrow(ResourceNotFoundException.withCode("DOCUMENT_NOT_FOUND", "檔案不存在"))
                    .when(documentService).deleteDocument(eq(tripId), eq(documentId), any(UUID.class));

            // When & Then
            mockMvc.perform(delete("/api/trips/{tripId}/documents/{documentId}", tripId, documentId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should return 403 when user has no permission")
        void deleteDocument_noPermission_shouldReturn403() throws Exception {
            // Given
            doThrow(new ForbiddenException("您沒有權限刪除此檔案"))
                    .when(documentService).deleteDocument(eq(tripId), eq(documentId), any(UUID.class));

            // When & Then
            mockMvc.perform(delete("/api/trips/{tripId}/documents/{documentId}", tripId, documentId)
                            .with(oauth2Login().oauth2User(userPrincipal))
                            .with(csrf()))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/documents/storage")
    class GetStorageUsageTests {

        @Test
        @DisplayName("should return storage usage with 200")
        void getStorageUsage_shouldReturn200() throws Exception {
            // Given
            long usedBytes = 50L * 1024 * 1024; // 50 MB
            when(documentService.getTripStorageUsage(eq(tripId), any(UUID.class)))
                    .thenReturn(usedBytes);

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/documents/storage", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.usedBytes").value(usedBytes))
                    .andExpect(jsonPath("$.data.maxBytes").value(Document.MAX_TRIP_STORAGE))
                    .andExpect(jsonPath("$.data.usagePercent").exists());
        }

        @Test
        @DisplayName("should return 403 when user has no permission")
        void getStorageUsage_noPermission_shouldReturn403() throws Exception {
            // Given
            when(documentService.getTripStorageUsage(eq(tripId), any(UUID.class)))
                    .thenThrow(new ForbiddenException("您沒有權限查看此行程"));

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/documents/storage", tripId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/trips/{tripId}/activities/{activityId}/documents")
    class GetDocumentsByActivityTests {

        @Test
        @DisplayName("should return documents linked to activity with 200")
        void getDocumentsByActivity_shouldReturn200() throws Exception {
            // Given
            List<DocumentResponse> documents = Arrays.asList(testDocumentResponse);
            when(documentService.getDocumentsByActivity(eq(activityId), eq(tripId), any(UUID.class)))
                    .thenReturn(documents);

            // When & Then
            mockMvc.perform(get("/api/trips/{tripId}/activities/{activityId}/documents", tripId, activityId)
                            .with(oauth2Login().oauth2User(userPrincipal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(documentId.toString()));
        }
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("should return 4xx when not authenticated")
        void uploadDocument_notAuthenticated_shouldReturn4xx() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "receipt.pdf",
                    "application/pdf",
                    "test content".getBytes()
            );

            // Spring Security returns 4xx for unauthenticated requests
            mockMvc.perform(multipart("/api/trips/{tripId}/documents", tripId)
                            .file(file)
                            .with(csrf()))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("should return 4xx for GET when not authenticated")
        void getDocuments_notAuthenticated_shouldReturn4xx() throws Exception {
            // Spring Security returns 4xx for unauthenticated requests
            mockMvc.perform(get("/api/trips/{tripId}/documents", tripId))
                    .andExpect(status().is4xxClientError());
        }
    }
}
