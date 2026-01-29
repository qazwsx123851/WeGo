package com.wego.service.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of StorageClient for testing and development.
 * Stores files in memory.
 *
 * @contract
 *   - Only active when SupabaseStorageClient is not available
 *   - Files are stored in memory and lost on restart
 *   - Useful for testing without external dependencies
 */
@Slf4j
@Component
@ConditionalOnMissingBean(SupabaseStorageClient.class)
public class MockStorageClient implements StorageClient {

    private final Map<String, byte[]> storage = new ConcurrentHashMap<>();
    private final Map<String, String> mimeTypes = new ConcurrentHashMap<>();

    @Override
    public String uploadFile(String bucket, String path, byte[] content, String mimeType) {
        String key = buildKey(bucket, path);
        storage.put(key, content);
        mimeTypes.put(key, mimeType);
        log.info("[MOCK] Uploaded file: {} ({} bytes, {})", key, content.length, mimeType);
        return "mock://storage/" + bucket + "/" + path;
    }

    @Override
    public byte[] downloadFile(String bucket, String path) {
        String key = buildKey(bucket, path);
        byte[] content = storage.get(key);
        if (content == null) {
            throw StorageException.fileNotFound(path);
        }
        log.debug("[MOCK] Downloaded file: {} ({} bytes)", key, content.length);
        return content;
    }

    @Override
    public void deleteFile(String bucket, String path) {
        String key = buildKey(bucket, path);
        storage.remove(key);
        mimeTypes.remove(key);
        log.info("[MOCK] Deleted file: {}", key);
    }

    @Override
    public String getSignedUrl(String bucket, String path, int expiresInSeconds) {
        String key = buildKey(bucket, path);
        if (!storage.containsKey(key)) {
            throw StorageException.fileNotFound(path);
        }
        String signedUrl = "mock://storage/" + bucket + "/" + path + "?token=mock-signed&expires=" + expiresInSeconds;
        log.debug("[MOCK] Generated signed URL for: {} (expires in {} seconds)", key, expiresInSeconds);
        return signedUrl;
    }

    @Override
    public boolean fileExists(String bucket, String path) {
        String key = buildKey(bucket, path);
        boolean exists = storage.containsKey(key);
        log.debug("[MOCK] File exists check for {}: {}", key, exists);
        return exists;
    }

    private String buildKey(String bucket, String path) {
        return bucket + "/" + path;
    }

    /**
     * Clears all stored files. Useful for testing.
     */
    public void clear() {
        storage.clear();
        mimeTypes.clear();
        log.info("[MOCK] Cleared all stored files");
    }

    /**
     * Gets the number of stored files. Useful for testing.
     */
    public int getFileCount() {
        return storage.size();
    }
}
