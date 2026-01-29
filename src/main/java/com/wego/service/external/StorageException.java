package com.wego.service.external;

/**
 * Exception thrown when storage operations fail.
 */
public class StorageException extends RuntimeException {

    private final String errorCode;

    public StorageException(String message) {
        super(message);
        this.errorCode = "STORAGE_ERROR";
    }

    public StorageException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "STORAGE_ERROR";
    }

    public StorageException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Creates a StorageException for file not found.
     */
    public static StorageException fileNotFound(String path) {
        return new StorageException("FILE_NOT_FOUND", "檔案不存在: " + path);
    }

    /**
     * Creates a StorageException for upload failure.
     */
    public static StorageException uploadFailed(String reason) {
        return new StorageException("UPLOAD_FAILED", "檔案上傳失敗: " + reason);
    }

    /**
     * Creates a StorageException for download failure.
     */
    public static StorageException downloadFailed(String reason) {
        return new StorageException("DOWNLOAD_FAILED", "檔案下載失敗: " + reason);
    }

    /**
     * Creates a StorageException for deletion failure.
     */
    public static StorageException deleteFailed(String reason) {
        return new StorageException("DELETE_FAILED", "檔案刪除失敗: " + reason);
    }
}
