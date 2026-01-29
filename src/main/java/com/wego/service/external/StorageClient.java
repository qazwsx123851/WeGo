package com.wego.service.external;

import java.io.InputStream;

/**
 * Interface for external storage operations.
 * Allows swapping between different storage providers (Supabase, S3, etc.).
 *
 * @contract
 *   - All methods throw StorageException on failure
 *   - File paths are relative to the bucket root
 */
public interface StorageClient {

    /**
     * Uploads a file to storage.
     *
     * @param bucket The bucket name
     * @param path The file path within the bucket
     * @param content The file content as byte array
     * @param mimeType The MIME type of the file
     * @return The public URL of the uploaded file
     * @throws StorageException if upload fails
     */
    String uploadFile(String bucket, String path, byte[] content, String mimeType);

    /**
     * Downloads a file from storage.
     *
     * @param bucket The bucket name
     * @param path The file path within the bucket
     * @return The file content as byte array
     * @throws StorageException if download fails or file not found
     */
    byte[] downloadFile(String bucket, String path);

    /**
     * Deletes a file from storage.
     *
     * @param bucket The bucket name
     * @param path The file path within the bucket
     * @throws StorageException if deletion fails
     */
    void deleteFile(String bucket, String path);

    /**
     * Generates a signed URL for temporary access to a file.
     *
     * @param bucket The bucket name
     * @param path The file path within the bucket
     * @param expiresInSeconds The expiry time in seconds
     * @return The signed URL
     * @throws StorageException if URL generation fails
     */
    String getSignedUrl(String bucket, String path, int expiresInSeconds);

    /**
     * Checks if a file exists in storage.
     *
     * @param bucket The bucket name
     * @param path The file path within the bucket
     * @return true if file exists
     */
    boolean fileExists(String bucket, String path);
}
