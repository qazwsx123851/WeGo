package com.wego.service.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wego.config.SupabaseProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Supabase Storage client for file operations.
 *
 * Uses Supabase Storage REST API:
 * - Upload: POST /storage/v1/object/{bucket}/{path}
 * - Download: GET /storage/v1/object/{bucket}/{path}
 * - Delete: DELETE /storage/v1/object/{bucket}/{paths}
 * - Signed URL: POST /storage/v1/object/sign/{bucket}/{path}
 *
 * @contract
 *   - pre: SupabaseProperties must be configured with valid url and serviceKey
 *   - post: All operations use service key for authentication
 *
 * @see StorageClient
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "wego.supabase.url")
public class SupabaseStorageClient implements StorageClient {

    private static final String STORAGE_BASE_PATH = "/storage/v1/object";

    private final SupabaseProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public SupabaseStorageClient(SupabaseProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();

        // Check if Supabase is properly configured
        this.enabled = properties.getUrl() != null &&
                       !properties.getUrl().isBlank() &&
                       properties.getServiceKey() != null &&
                       !properties.getServiceKey().isBlank();

        if (!enabled) {
            log.warn("Supabase Storage is NOT properly configured. " +
                     "URL: {}, ServiceKey: {}",
                     properties.getUrl() != null ? "set" : "missing",
                     properties.getServiceKey() != null ? "set" : "missing");
        } else {
            log.info("Supabase Storage initialized with URL: {}", properties.getUrl());
        }
    }

    /**
     * Checks if Supabase storage is properly configured.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * {@inheritDoc}
     *
     * @contract
     *   - pre: bucket != null, path != null, content != null
     *   - post: Returns public URL of uploaded file
     *   - throws: StorageException if upload fails
     */
    @Override
    public String uploadFile(String bucket, String path, byte[] content, String mimeType) {
        if (!enabled) {
            log.error("Cannot upload file: Supabase Storage is not configured. " +
                      "Please set SUPABASE_URL and SUPABASE_SERVICE_KEY environment variables.");
            throw StorageException.uploadFailed("Supabase Storage is not configured");
        }

        log.debug("Uploading file to {}/{}", bucket, path);

        String url = buildStorageUrl(bucket, path);

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));

        HttpEntity<byte[]> entity = new HttpEntity<>(content, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully uploaded file to {}/{}", bucket, path);
                return buildPublicUrl(bucket, path);
            } else {
                throw StorageException.uploadFailed("Unexpected response: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            log.error("Failed to upload file to {}/{}: {}", bucket, path, e.getMessage());
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                // File already exists, try to update
                return updateFile(bucket, path, content, mimeType);
            }
            throw StorageException.uploadFailed(e.getMessage());
        } catch (Exception e) {
            log.error("Failed to upload file to {}/{}", bucket, path, e);
            throw StorageException.uploadFailed(e.getMessage());
        }
    }

    /**
     * Updates an existing file in storage.
     */
    private String updateFile(String bucket, String path, byte[] content, String mimeType) {
        log.debug("Updating existing file at {}/{}", bucket, path);

        String url = buildStorageUrl(bucket, path);

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));

        HttpEntity<byte[]> entity = new HttpEntity<>(content, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully updated file at {}/{}", bucket, path);
                return buildPublicUrl(bucket, path);
            } else {
                throw StorageException.uploadFailed("Unexpected response: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to update file at {}/{}", bucket, path, e);
            throw StorageException.uploadFailed(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte[] downloadFile(String bucket, String path) {
        log.debug("Downloading file from {}/{}", bucket, path);

        String url = buildStorageUrl(bucket, path);

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("Successfully downloaded file from {}/{}", bucket, path);
                return response.getBody();
            } else {
                throw StorageException.downloadFailed("Unexpected response: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw StorageException.fileNotFound(path);
        } catch (Exception e) {
            log.error("Failed to download file from {}/{}", bucket, path, e);
            throw StorageException.downloadFailed(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFile(String bucket, String path) {
        log.debug("Deleting file from {}/{}", bucket, path);

        // Supabase delete endpoint expects a JSON body with prefixes array
        String url = properties.getUrl() + STORAGE_BASE_PATH + "/" + bucket;

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("prefixes", new String[]{path});

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Successfully deleted file from {}/{}", bucket, path);
            } else {
                throw StorageException.deleteFailed("Unexpected response: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("File not found for deletion: {}/{}", bucket, path);
            // Silently ignore - file doesn't exist anyway
        } catch (Exception e) {
            log.error("Failed to delete file from {}/{}", bucket, path, e);
            throw StorageException.deleteFailed(e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSignedUrl(String bucket, String path, int expiresInSeconds) {
        log.debug("Generating signed URL for {}/{} (expires in {} seconds)", bucket, path, expiresInSeconds);

        String url = properties.getUrl() + STORAGE_BASE_PATH + "/sign/" + bucket + "/" + path;

        HttpHeaders headers = createAuthHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("expiresIn", expiresInSeconds);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode json = objectMapper.readTree(response.getBody());
                String signedURL = json.path("signedURL").asText();

                if (signedURL != null && !signedURL.isEmpty()) {
                    // If the signed URL is relative, prepend the base URL
                    if (signedURL.startsWith("/")) {
                        signedURL = properties.getUrl() + signedURL;
                    }
                    log.debug("Generated signed URL for {}/{}", bucket, path);
                    return signedURL;
                }
            }
            throw new StorageException("SIGNED_URL_FAILED", "無法生成簽名 URL");
        } catch (StorageException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to generate signed URL for {}/{}", bucket, path, e);
            throw new StorageException("SIGNED_URL_FAILED", "無法生成簽名 URL: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean fileExists(String bucket, String path) {
        log.debug("Checking if file exists: {}/{}", bucket, path);

        String url = buildStorageUrl(bucket, path);

        HttpHeaders headers = createAuthHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    entity,
                    Void.class
            );
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.warn("Error checking file existence: {}/{}", bucket, path, e);
            return false;
        }
    }

    /**
     * Creates HTTP headers with Supabase authentication.
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + properties.getServiceKey());
        headers.set("apikey", properties.getServiceKey());
        return headers;
    }

    /**
     * Builds the storage API URL for a file.
     */
    private String buildStorageUrl(String bucket, String path) {
        return properties.getUrl() + STORAGE_BASE_PATH + "/" + bucket + "/" + path;
    }

    /**
     * Builds the public URL for an uploaded file.
     */
    private String buildPublicUrl(String bucket, String path) {
        return properties.getUrl() + STORAGE_BASE_PATH + "/public/" + bucket + "/" + path;
    }
}
