package com.wego.service.external;

import com.wego.config.SupabaseProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SupabaseStorageClient")
class SupabaseStorageClientTest {

    @Mock
    private RestTemplate apiRestTemplate;

    @Mock
    private RestTemplate fileRestTemplate;

    private SupabaseStorageClient client;
    private SupabaseProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SupabaseProperties();
        properties.setUrl("https://test.supabase.co");
        properties.setServiceKey("test-service-key");
        properties.setStorageBucket("documents");
        client = new SupabaseStorageClient(properties, apiRestTemplate, fileRestTemplate);
    }

    @Nested
    @DisplayName("isEnabled")
    class IsEnabled {

        @Test
        @DisplayName("should be enabled with valid config")
        void shouldBeEnabled() {
            assertThat(client.isEnabled()).isTrue();
        }

        @Test
        @DisplayName("should be disabled without URL")
        void shouldBeDisabledWithoutUrl() {
            SupabaseProperties noUrl = new SupabaseProperties();
            noUrl.setServiceKey("key");
            SupabaseStorageClient disabledClient =
                    new SupabaseStorageClient(noUrl, apiRestTemplate, fileRestTemplate);
            assertThat(disabledClient.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("should be disabled without service key")
        void shouldBeDisabledWithoutKey() {
            SupabaseProperties noKey = new SupabaseProperties();
            noKey.setUrl("https://test.supabase.co");
            SupabaseStorageClient disabledClient =
                    new SupabaseStorageClient(noKey, apiRestTemplate, fileRestTemplate);
            assertThat(disabledClient.isEnabled()).isFalse();
        }
    }

    @Nested
    @DisplayName("uploadFile")
    class UploadFile {

        @Test
        @DisplayName("should return public URL on successful upload")
        void shouldReturnPublicUrl() {
            when(fileRestTemplate.exchange(
                    anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok("{}"));

            String url = client.uploadFile("documents", "trip1/file.pdf",
                    "content".getBytes(), "application/pdf");

            assertThat(url).contains("test.supabase.co")
                    .contains("public")
                    .contains("documents")
                    .contains("trip1/file.pdf");
        }

        @Test
        @DisplayName("should update file on conflict (409)")
        void shouldUpdateOnConflict() {
            when(fileRestTemplate.exchange(
                    anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new HttpClientErrorException(HttpStatus.CONFLICT));
            when(fileRestTemplate.exchange(
                    anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok("{}"));

            String url = client.uploadFile("documents", "trip1/file.pdf",
                    "content".getBytes(), "application/pdf");

            assertThat(url).contains("public");
        }

        @Test
        @DisplayName("should throw when not configured")
        void shouldThrowWhenNotConfigured() {
            SupabaseProperties noKey = new SupabaseProperties();
            noKey.setUrl("https://test.supabase.co");
            SupabaseStorageClient disabledClient =
                    new SupabaseStorageClient(noKey, apiRestTemplate, fileRestTemplate);

            assertThatThrownBy(() -> disabledClient.uploadFile(
                    "documents", "path", "content".getBytes(), "application/pdf"))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining("not configured");
        }

        @Test
        @DisplayName("should throw on network error")
        void shouldThrowOnNetworkError() {
            when(fileRestTemplate.exchange(
                    anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new ResourceAccessException("Connection refused"));

            assertThatThrownBy(() -> client.uploadFile(
                    "documents", "path", "content".getBytes(), "application/pdf"))
                    .isInstanceOf(StorageException.class);
        }
    }

    @Nested
    @DisplayName("downloadFile")
    class DownloadFile {

        @Test
        @DisplayName("should return file bytes on success")
        void shouldReturnBytes() {
            byte[] expectedContent = "file content".getBytes();
            when(fileRestTemplate.exchange(
                    anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                    .thenReturn(ResponseEntity.ok(expectedContent));

            byte[] result = client.downloadFile("documents", "trip1/file.pdf");

            assertThat(result).isEqualTo(expectedContent);
        }

        @Test
        @DisplayName("should throw on file not found")
        void shouldThrowOnNotFound() {
            when(fileRestTemplate.exchange(
                    anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.NOT_FOUND, "Not Found", null, null, null));

            assertThatThrownBy(() -> client.downloadFile("documents", "trip1/missing.pdf"))
                    .isInstanceOf(StorageException.class);
        }

        @Test
        @DisplayName("should throw on network error")
        void shouldThrowOnNetworkError() {
            when(fileRestTemplate.exchange(
                    anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                    .thenThrow(new ResourceAccessException("timeout"));

            assertThatThrownBy(() -> client.downloadFile("documents", "trip1/file.pdf"))
                    .isInstanceOf(StorageException.class);
        }
    }

    @Nested
    @DisplayName("deleteFile")
    class DeleteFile {

        @Test
        @DisplayName("should succeed on successful delete")
        void shouldSucceedOnDelete() {
            when(apiRestTemplate.exchange(
                    anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok("[]"));

            // Should not throw
            client.deleteFile("documents", "trip1/file.pdf");
        }

        @Test
        @DisplayName("should silently ignore file not found")
        void shouldIgnoreNotFound() {
            when(apiRestTemplate.exchange(
                    anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.NOT_FOUND, "Not Found", null, null, null));

            // Should not throw
            client.deleteFile("documents", "trip1/missing.pdf");
        }

        @Test
        @DisplayName("should throw on other errors")
        void shouldThrowOnOtherErrors() {
            when(apiRestTemplate.exchange(
                    anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new ResourceAccessException("timeout"));

            assertThatThrownBy(() -> client.deleteFile("documents", "trip1/file.pdf"))
                    .isInstanceOf(StorageException.class);
        }
    }

    @Nested
    @DisplayName("getSignedUrl")
    class GetSignedUrl {

        @Test
        @DisplayName("should return full signed URL")
        void shouldReturnSignedUrl() {
            String signedUrlResponse = """
                    {"signedURL": "/object/sign/documents/trip1/file.pdf?token=abc123"}
                    """;
            when(apiRestTemplate.exchange(
                    anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(signedUrlResponse));

            String url = client.getSignedUrl("documents", "trip1/file.pdf", 3600);

            assertThat(url).startsWith("https://test.supabase.co/storage/v1");
            assertThat(url).contains("token=abc123");
        }

        @Test
        @DisplayName("should throw on API error")
        void shouldThrowOnError() {
            when(apiRestTemplate.exchange(
                    anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new ResourceAccessException("timeout"));

            assertThatThrownBy(() -> client.getSignedUrl("documents", "trip1/file.pdf", 3600))
                    .isInstanceOf(StorageException.class);
        }
    }

    @Nested
    @DisplayName("fileExists")
    class FileExists {

        @Test
        @DisplayName("should return true when file exists")
        void shouldReturnTrueWhenExists() {
            when(apiRestTemplate.exchange(
                    anyString(), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(Void.class)))
                    .thenReturn(ResponseEntity.ok().build());

            assertThat(client.fileExists("documents", "trip1/file.pdf")).isTrue();
        }

        @Test
        @DisplayName("should return false when file not found")
        void shouldReturnFalseWhenNotFound() {
            when(apiRestTemplate.exchange(
                    anyString(), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(Void.class)))
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.NOT_FOUND, "Not Found", null, null, null));

            assertThat(client.fileExists("documents", "trip1/missing.pdf")).isFalse();
        }

        @Test
        @DisplayName("should return false on network error")
        void shouldReturnFalseOnError() {
            when(apiRestTemplate.exchange(
                    anyString(), eq(HttpMethod.HEAD), any(HttpEntity.class), eq(Void.class)))
                    .thenThrow(new ResourceAccessException("timeout"));

            assertThat(client.fileExists("documents", "trip1/file.pdf")).isFalse();
        }
    }
}
