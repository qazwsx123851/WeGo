package com.wego.service;

import com.wego.config.SupabaseProperties;
import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreateDocumentRequest;
import com.wego.dto.response.DocumentResponse;
import com.wego.entity.Activity;
import com.wego.entity.Document;
import com.wego.entity.Place;
import com.wego.entity.Role;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.exception.ValidationException;
import com.wego.repository.ActivityRepository;
import com.wego.repository.DocumentRepository;
import com.wego.repository.PlaceRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import com.wego.service.external.StorageClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DocumentService.
 *
 * Test cases based on docs/test-cases.md:
 * - F-001: Upload valid file
 * - F-002: Upload exceeds file size limit
 * - F-003: Upload exceeds trip storage limit
 * - F-004: Upload unsupported format
 * - F-005: Download by member
 * - F-006: Download by non-member (denied)
 * - F-007: Delete by uploader
 * - F-008: Delete by OWNER
 * - F-009: Delete by VIEWER (denied)
 *
 * @contract
 *   - Tests follow TDD methodology
 *   - Covers all public methods
 *   - Tests edge cases and error scenarios
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageClient storageClient;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private SupabaseProperties supabaseProperties;

    @InjectMocks
    private DocumentService documentService;

    private UUID tripId;
    private UUID userId;
    private UUID documentId;
    private UUID ownerId;
    private User testUser;
    private User ownerUser;
    private Document testDocument;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();
        documentId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .build();

        ownerUser = User.builder()
                .id(ownerId)
                .email("owner@example.com")
                .nickname("Owner")
                .build();

        testDocument = Document.builder()
                .id(documentId)
                .tripId(tripId)
                .fileName("test-uuid.pdf")
                .originalFileName("receipt.pdf")
                .fileUrl("https://storage.example.com/documents/test-uuid.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .uploadedBy(userId)
                .build();

        // Default mock behaviors
        when(supabaseProperties.getStorageBucket()).thenReturn("documents");
        when(supabaseProperties.getSignedUrlExpiry()).thenReturn(3600);
        when(tripRepository.existsById(tripId)).thenReturn(true);

        // Manually invoke @PostConstruct since Mockito doesn't call it
        documentService.initSignedUrlCache();
    }

    @Nested
    @DisplayName("uploadDocument - 檔案上傳測試")
    class UploadDocumentTests {

        @Test
        @DisplayName("F-001: 上傳有效的 PDF 檔案應成功")
        void uploadDocument_withValidPdf_shouldSucceed() {
            // Given - PDF magic bytes: %PDF (0x25 0x50 0x44 0x46)
            byte[] pdfContent = new byte[1024];
            pdfContent[0] = 0x25; // %
            pdfContent[1] = 0x50; // P
            pdfContent[2] = 0x44; // D
            pdfContent[3] = 0x46; // F
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "receipt.pdf",
                    "application/pdf",
                    pdfContent
            );
            CreateDocumentRequest request = CreateDocumentRequest.builder()
                    .description("Test receipt")
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(documentRepository.sumFileSizeByTripId(tripId)).thenReturn(0L);
            when(storageClient.uploadFile(anyString(), anyString(), any(byte[].class), anyString()))
                    .thenReturn("https://storage.example.com/documents/uuid.pdf");
            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(documentId);
                return doc;
            });
            when(userRepository.findAllById(any())).thenReturn(List.of(testUser));

            // When
            DocumentResponse response = documentService.uploadDocument(tripId, userId, file, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOriginalFileName()).isEqualTo("receipt.pdf");
            assertThat(response.getMimeType()).isEqualTo("application/pdf");
            assertThat(response.getDescription()).isEqualTo("Test receipt");
            assertThat(response.isPdf()).isTrue();
            verify(storageClient).uploadFile(anyString(), anyString(), any(byte[].class), eq("application/pdf"));
            verify(documentRepository).save(any(Document.class));
        }

        @Test
        @DisplayName("F-001: 上傳有效的圖片檔案應成功")
        void uploadDocument_withValidImage_shouldSucceed() {
            // Given - JPEG magic bytes: FF D8 FF E0
            byte[] jpegContent = new byte[2048];
            jpegContent[0] = (byte) 0xFF;
            jpegContent[1] = (byte) 0xD8;
            jpegContent[2] = (byte) 0xFF;
            jpegContent[3] = (byte) 0xE0;
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "photo.jpg",
                    "image/jpeg",
                    jpegContent
            );
            CreateDocumentRequest request = CreateDocumentRequest.builder().build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(documentRepository.sumFileSizeByTripId(tripId)).thenReturn(0L);
            when(storageClient.uploadFile(anyString(), anyString(), any(byte[].class), anyString()))
                    .thenReturn("https://storage.example.com/documents/uuid.jpg");
            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(documentId);
                return doc;
            });
            when(userRepository.findAllById(any())).thenReturn(List.of(testUser));

            // When
            DocumentResponse response = documentService.uploadDocument(tripId, userId, file, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getOriginalFileName()).isEqualTo("photo.jpg");
            assertThat(response.isImage()).isTrue();
        }

        @Test
        @DisplayName("F-002: 上傳超過檔案大小限制 (10MB) 應拋出 ValidationException")
        void uploadDocument_exceedingFileSize_shouldThrowValidationException() {
            // Given
            byte[] largeContent = new byte[11 * 1024 * 1024]; // 11 MB
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "large.pdf",
                    "application/pdf",
                    largeContent
            );
            CreateDocumentRequest request = CreateDocumentRequest.builder().build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> documentService.uploadDocument(tripId, userId, file, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("檔案大小超過限制");
        }

        @Test
        @DisplayName("F-003: 上傳導致行程總容量超過 100MB 應拋出 ValidationException")
        void uploadDocument_exceedingTripStorage_shouldThrowValidationException() {
            // Given - PDF magic bytes: %PDF
            byte[] pdfContent = new byte[5 * 1024 * 1024]; // 5 MB
            pdfContent[0] = 0x25; // %
            pdfContent[1] = 0x50; // P
            pdfContent[2] = 0x44; // D
            pdfContent[3] = 0x46; // F
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "receipt.pdf",
                    "application/pdf",
                    pdfContent
            );
            CreateDocumentRequest request = CreateDocumentRequest.builder().build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(documentRepository.sumFileSizeByTripId(tripId)).thenReturn(96L * 1024 * 1024); // 96 MB already

            // When/Then
            assertThatThrownBy(() -> documentService.uploadDocument(tripId, userId, file, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("行程儲存空間已滿");
        }

        @Test
        @DisplayName("F-004: 上傳不支援的檔案格式應拋出 ValidationException")
        void uploadDocument_unsupportedFormat_shouldThrowValidationException() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "document.exe",
                    "application/x-msdownload",
                    new byte[1024]
            );
            CreateDocumentRequest request = CreateDocumentRequest.builder().build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> documentService.uploadDocument(tripId, userId, file, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("不支援的檔案格式");
        }

        @Test
        @DisplayName("無編輯權限應拋出 ForbiddenException")
        void uploadDocument_noEditPermission_shouldThrowForbiddenException() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "receipt.pdf",
                    "application/pdf",
                    new byte[1024]
            );
            CreateDocumentRequest request = CreateDocumentRequest.builder().build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> documentService.uploadDocument(tripId, userId, file, request))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("沒有權限");
        }

        @Test
        @DisplayName("行程不存在應拋出 ResourceNotFoundException")
        void uploadDocument_tripNotFound_shouldThrowResourceNotFoundException() {
            // Given
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "receipt.pdf",
                    "application/pdf",
                    new byte[1024]
            );
            CreateDocumentRequest request = CreateDocumentRequest.builder().build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.existsById(tripId)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> documentService.uploadDocument(tripId, userId, file, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("上傳帶有活動關聯的檔案應正確設定 relatedActivityId")
        void uploadDocument_withActivityLink_shouldSetRelatedActivityId() {
            // Given - PDF magic bytes: %PDF
            UUID activityId = UUID.randomUUID();
            byte[] pdfContent = new byte[1024];
            pdfContent[0] = 0x25; // %
            pdfContent[1] = 0x50; // P
            pdfContent[2] = 0x44; // D
            pdfContent[3] = 0x46; // F
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "receipt.pdf",
                    "application/pdf",
                    pdfContent
            );
            CreateDocumentRequest request = CreateDocumentRequest.builder()
                    .relatedActivityId(activityId)
                    .relatedDay(1)
                    .build();

            UUID placeId = UUID.randomUUID();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(documentRepository.sumFileSizeByTripId(tripId)).thenReturn(0L);
            when(storageClient.uploadFile(anyString(), anyString(), any(byte[].class), anyString()))
                    .thenReturn("https://storage.example.com/documents/uuid.pdf");
            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(documentId);
                return doc;
            });
            when(userRepository.findAllById(any())).thenReturn(List.of(testUser));

            Activity mockActivity = Activity.builder().id(activityId).placeId(placeId).build();
            when(activityRepository.findAllById(any())).thenReturn(List.of(mockActivity));

            Place mockPlace = Place.builder().id(placeId).name("測試景點").build();
            when(placeRepository.findAllById(any())).thenReturn(List.of(mockPlace));

            // When
            DocumentResponse response = documentService.uploadDocument(tripId, userId, file, request);

            // Then
            assertThat(response.getRelatedActivityId()).isEqualTo(activityId);
            assertThat(response.getRelatedDay()).isEqualTo(1);
            assertThat(response.getRelatedActivityName()).isEqualTo("測試景點");

            ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
            verify(documentRepository).save(docCaptor.capture());
            assertThat(docCaptor.getValue().getRelatedActivityId()).isEqualTo(activityId);
        }
    }

    @Nested
    @DisplayName("getDocumentsByTrip - 取得行程文件列表測試")
    class GetDocumentsByTripTests {

        @Test
        @DisplayName("成員可以取得行程文件列表")
        void getDocumentsByTrip_asMember_shouldReturnDocuments() {
            // Given
            Document doc1 = Document.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .fileName("doc1.pdf")
                    .originalFileName("receipt1.pdf")
                    .fileUrl("https://storage.example.com/doc1.pdf")
                    .fileSize(1024L)
                    .mimeType("application/pdf")
                    .uploadedBy(userId)
                    .build();
            Document doc2 = Document.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .fileName("doc2.jpg")
                    .originalFileName("photo.jpg")
                    .fileUrl("https://storage.example.com/doc2.jpg")
                    .fileSize(2048L)
                    .mimeType("image/jpeg")
                    .uploadedBy(userId)
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(documentRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Arrays.asList(doc1, doc2));
            when(userRepository.findAllById(any())).thenReturn(List.of(testUser));

            // When
            List<DocumentResponse> documents = documentService.getDocumentsByTrip(tripId, userId);

            // Then
            assertThat(documents).hasSize(2);
            assertThat(documents.get(0).getOriginalFileName()).isEqualTo("receipt1.pdf");
            assertThat(documents.get(1).getOriginalFileName()).isEqualTo("photo.jpg");
        }

        @Test
        @DisplayName("F-006: 非成員無法取得文件列表")
        void getDocumentsByTrip_asNonMember_shouldThrowForbiddenException() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> documentService.getDocumentsByTrip(tripId, userId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("沒有權限");
        }

        @Test
        @DisplayName("行程沒有文件時應返回空列表")
        void getDocumentsByTrip_noDocuments_shouldReturnEmptyList() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(documentRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.emptyList());

            // When
            List<DocumentResponse> documents = documentService.getDocumentsByTrip(tripId, userId);

            // Then
            assertThat(documents).isEmpty();
        }
    }

    @Nested
    @DisplayName("getDocumentsByTrip with signedUrls - Signed URL 測試")
    class GetDocumentsByTripWithSignedUrlsTests {

        @Test
        @DisplayName("includeSignedUrls=true 時圖片和 PDF 應有 signedUrl")
        void getDocumentsByTrip_withSignedUrls_shouldPopulateSignedUrl() {
            // Given
            Document imageDoc = Document.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .fileName("photo.jpg")
                    .originalFileName("photo.jpg")
                    .fileUrl("https://storage.example.com/photo.jpg")
                    .fileSize(2048L)
                    .mimeType("image/jpeg")
                    .uploadedBy(userId)
                    .build();
            Document pdfDoc = Document.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .fileName("doc.pdf")
                    .originalFileName("doc.pdf")
                    .fileUrl("https://storage.example.com/doc.pdf")
                    .fileSize(1024L)
                    .mimeType("application/pdf")
                    .uploadedBy(userId)
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(documentRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(imageDoc, pdfDoc));
            when(userRepository.findAllById(any())).thenReturn(List.of(testUser));
            when(storageClient.getSignedUrl(anyString(), anyString(), anyInt()))
                    .thenReturn("https://storage.example.com/signed/url");

            // When
            List<DocumentResponse> documents = documentService.getDocumentsByTrip(tripId, userId, true);

            // Then
            assertThat(documents).hasSize(2);
            assertThat(documents.get(0).getSignedUrl()).isEqualTo("https://storage.example.com/signed/url");
            assertThat(documents.get(1).getSignedUrl()).isEqualTo("https://storage.example.com/signed/url");
        }

        @Test
        @DisplayName("includeSignedUrls=false 時不應有 signedUrl")
        void getDocumentsByTrip_withoutSignedUrls_shouldNotPopulateSignedUrl() {
            // Given
            Document imageDoc = Document.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .fileName("photo.jpg")
                    .originalFileName("photo.jpg")
                    .fileUrl("https://storage.example.com/photo.jpg")
                    .fileSize(2048L)
                    .mimeType("image/jpeg")
                    .uploadedBy(userId)
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(documentRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(imageDoc));
            when(userRepository.findAllById(any())).thenReturn(List.of(testUser));

            // When
            List<DocumentResponse> documents = documentService.getDocumentsByTrip(tripId, userId, false);

            // Then
            assertThat(documents).hasSize(1);
            assertThat(documents.get(0).getSignedUrl()).isNull();
            verify(storageClient, never()).getSignedUrl(anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("Signed URL 生成失敗時應優雅降級 (signedUrl = null)")
        void getDocumentsByTrip_signedUrlFailure_shouldGracefullyDegrade() {
            // Given
            Document imageDoc = Document.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .fileName("photo.jpg")
                    .originalFileName("photo.jpg")
                    .fileUrl("https://storage.example.com/photo.jpg")
                    .fileSize(2048L)
                    .mimeType("image/jpeg")
                    .uploadedBy(userId)
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(documentRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(imageDoc));
            when(userRepository.findAllById(any())).thenReturn(List.of(testUser));
            when(storageClient.getSignedUrl(anyString(), anyString(), anyInt()))
                    .thenThrow(new RuntimeException("Supabase unavailable"));

            // When
            List<DocumentResponse> documents = documentService.getDocumentsByTrip(tripId, userId, true);

            // Then — response returned successfully, signedUrl is null
            assertThat(documents).hasSize(1);
            assertThat(documents.get(0).getSignedUrl()).isNull();
            assertThat(documents.get(0).getOriginalFileName()).isEqualTo("photo.jpg");
        }

        @Test
        @DisplayName("Signed URL 快取命中時不應重複呼叫 storageClient")
        void getDocumentsByTrip_signedUrlCacheHit_shouldNotCallStorageClientAgain() {
            // Given
            Document imageDoc = Document.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .fileName("photo.jpg")
                    .originalFileName("photo.jpg")
                    .fileUrl("https://storage.example.com/photo.jpg")
                    .fileSize(2048L)
                    .mimeType("image/jpeg")
                    .uploadedBy(userId)
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(documentRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(List.of(imageDoc));
            when(userRepository.findAllById(any())).thenReturn(List.of(testUser));
            when(storageClient.getSignedUrl(anyString(), anyString(), anyInt()))
                    .thenReturn("https://storage.example.com/signed/url");

            // When — call twice
            documentService.getDocumentsByTrip(tripId, userId, true);
            documentService.getDocumentsByTrip(tripId, userId, true);

            // Then — storageClient.getSignedUrl called only once (second time hits cache)
            verify(storageClient, org.mockito.Mockito.times(1))
                    .getSignedUrl(anyString(), anyString(), anyInt());
        }
    }

    @Nested
    @DisplayName("getDocument - 取得單一文件測試")
    class GetDocumentTests {

        @Test
        @DisplayName("F-005: 成員可以取得文件詳情")
        void getDocument_asMember_shouldReturnDocument() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(userRepository.findAllById(any())).thenReturn(List.of(testUser));

            // When
            DocumentResponse response = documentService.getDocument(tripId, documentId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(documentId);
            assertThat(response.getOriginalFileName()).isEqualTo("receipt.pdf");
        }

        @Test
        @DisplayName("文件不存在應拋出 ResourceNotFoundException")
        void getDocument_notFound_shouldThrowResourceNotFoundException() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> documentService.getDocument(tripId, documentId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("F-006: 非成員無法取得文件詳情")
        void getDocument_asNonMember_shouldThrowForbiddenException() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> documentService.getDocument(tripId, documentId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("文件不屬於指定行程應拋出 ResourceNotFoundException")
        void getDocument_wrongTrip_shouldThrowResourceNotFoundException() {
            // Given
            UUID wrongTripId = UUID.randomUUID();
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));

            // When/Then
            assertThatThrownBy(() -> documentService.getDocument(wrongTripId, documentId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("檔案不存在");
        }
    }

    @Nested
    @DisplayName("getDownloadUrl - 取得下載連結測試")
    class GetDownloadUrlTests {

        @Test
        @DisplayName("成員可以取得下載連結")
        void getDownloadUrl_asMember_shouldReturnSignedUrl() {
            // Given
            String expectedUrl = "https://storage.example.com/signed/documents/test-uuid.pdf?token=xxx";
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(storageClient.getSignedUrl(eq("documents"), anyString(), anyInt()))
                    .thenReturn(expectedUrl);

            // When
            String downloadUrl = documentService.getDownloadUrl(tripId, documentId, userId);

            // Then
            assertThat(downloadUrl).isEqualTo(expectedUrl);
            verify(storageClient).getSignedUrl(eq("documents"), anyString(), eq(3600));
        }

        @Test
        @DisplayName("非成員無法取得下載連結")
        void getDownloadUrl_asNonMember_shouldThrowForbiddenException() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> documentService.getDownloadUrl(tripId, documentId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("文件不屬於指定行程應拋出 ResourceNotFoundException")
        void getDownloadUrl_wrongTrip_shouldThrowResourceNotFoundException() {
            // Given
            UUID wrongTripId = UUID.randomUUID();
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));

            // When/Then
            assertThatThrownBy(() -> documentService.getDownloadUrl(wrongTripId, documentId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteDocument - 刪除文件測試")
    class DeleteDocumentTests {

        @Test
        @DisplayName("F-007: 上傳者可以刪除自己的文件")
        void deleteDocument_byUploader_shouldSucceed() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);

            // When
            documentService.deleteDocument(tripId, documentId, userId);

            // Then
            verify(storageClient).deleteFile(eq("documents"), anyString());
            verify(documentRepository).delete(testDocument);
        }

        @Test
        @DisplayName("F-008: 行程 OWNER 可以刪除任何成員的文件")
        void deleteDocument_byOwner_shouldSucceed() {
            // Given
            Document otherUserDocument = Document.builder()
                    .id(documentId)
                    .tripId(tripId)
                    .fileName("other-doc.pdf")
                    .originalFileName("other.pdf")
                    .fileUrl("https://storage.example.com/other.pdf")
                    .fileSize(1024L)
                    .mimeType("application/pdf")
                    .uploadedBy(UUID.randomUUID()) // Different user uploaded
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(otherUserDocument));
            when(permissionChecker.canView(tripId, ownerId)).thenReturn(true);
            when(permissionChecker.canDelete(tripId, ownerId)).thenReturn(true); // Owner permission

            // When
            documentService.deleteDocument(tripId, documentId, ownerId);

            // Then
            verify(storageClient).deleteFile(eq("documents"), anyString());
            verify(documentRepository).delete(otherUserDocument);
        }

        @Test
        @DisplayName("F-009: VIEWER 無法刪除文件")
        void deleteDocument_byViewer_shouldThrowForbiddenException() {
            // Given
            UUID viewerId = UUID.randomUUID();
            Document ownerDocument = Document.builder()
                    .id(documentId)
                    .tripId(tripId)
                    .fileName("owner-doc.pdf")
                    .originalFileName("owner.pdf")
                    .fileUrl("https://storage.example.com/owner.pdf")
                    .fileSize(1024L)
                    .mimeType("application/pdf")
                    .uploadedBy(ownerId) // Owner uploaded
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(ownerDocument));
            when(permissionChecker.canView(tripId, viewerId)).thenReturn(true);
            when(permissionChecker.canDelete(tripId, viewerId)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> documentService.deleteDocument(tripId, documentId, viewerId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("沒有權限刪除");
        }

        @Test
        @DisplayName("文件不存在應拋出 ResourceNotFoundException")
        void deleteDocument_notFound_shouldThrowResourceNotFoundException() {
            // Given
            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> documentService.deleteDocument(tripId, documentId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("EDITOR 可以刪除自己上傳的文件")
        void deleteDocument_editorOwnDocument_shouldSucceed() {
            // Given
            UUID editorId = UUID.randomUUID();
            Document editorDocument = Document.builder()
                    .id(documentId)
                    .tripId(tripId)
                    .fileName("editor-doc.pdf")
                    .originalFileName("editor.pdf")
                    .fileUrl("https://storage.example.com/editor.pdf")
                    .fileSize(1024L)
                    .mimeType("application/pdf")
                    .uploadedBy(editorId)
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(editorDocument));
            when(permissionChecker.canView(tripId, editorId)).thenReturn(true);
            when(permissionChecker.canDelete(tripId, editorId)).thenReturn(false); // Not owner

            // When
            documentService.deleteDocument(tripId, documentId, editorId);

            // Then
            verify(storageClient).deleteFile(eq("documents"), anyString());
            verify(documentRepository).delete(editorDocument);
        }

        @Test
        @DisplayName("EDITOR 無法刪除他人上傳的文件")
        void deleteDocument_editorOtherDocument_shouldThrowForbiddenException() {
            // Given
            UUID editorId = UUID.randomUUID();
            Document otherDocument = Document.builder()
                    .id(documentId)
                    .tripId(tripId)
                    .fileName("other-doc.pdf")
                    .originalFileName("other.pdf")
                    .fileUrl("https://storage.example.com/other.pdf")
                    .fileSize(1024L)
                    .mimeType("application/pdf")
                    .uploadedBy(UUID.randomUUID()) // Different user
                    .build();

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(otherDocument));
            when(permissionChecker.canView(tripId, editorId)).thenReturn(true);
            when(permissionChecker.canDelete(tripId, editorId)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> documentService.deleteDocument(tripId, documentId, editorId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("沒有權限刪除");
        }

        @Test
        @DisplayName("文件不屬於指定行程應拋出 ResourceNotFoundException")
        void deleteDocument_wrongTrip_shouldThrowResourceNotFoundException() {
            // Given
            UUID wrongTripId = UUID.randomUUID();
            when(documentRepository.findById(documentId)).thenReturn(Optional.of(testDocument));

            // When/Then
            assertThatThrownBy(() -> documentService.deleteDocument(wrongTripId, documentId, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("檔案不存在");
        }
    }

    @Nested
    @DisplayName("getTripStorageUsage - 取得行程儲存空間使用量")
    class GetTripStorageUsageTests {

        @Test
        @DisplayName("應正確計算儲存空間使用量")
        void getTripStorageUsage_shouldReturnCorrectUsage() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(documentRepository.sumFileSizeByTripId(tripId)).thenReturn(50L * 1024 * 1024); // 50 MB

            // When
            long usage = documentService.getTripStorageUsage(tripId, userId);

            // Then
            assertThat(usage).isEqualTo(50L * 1024 * 1024);
        }

        @Test
        @DisplayName("非成員無法查詢儲存空間")
        void getTripStorageUsage_asNonMember_shouldThrowForbiddenException() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> documentService.getTripStorageUsage(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
