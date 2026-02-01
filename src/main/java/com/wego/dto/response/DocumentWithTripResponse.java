package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for document with trip information in global view.
 *
 * @contract
 *   - Extends document info with trip title and uploader name
 *   - Used in global document list where trip context is needed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentWithTripResponse {

    private UUID id;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private long fileSize;
    private String mimeType;
    private boolean isImage;
    private boolean isPdf;
    private UUID tripId;
    private String tripTitle;
    private String uploadedByName;
    private String uploadedByAvatarUrl;
    private Instant createdAt;

    /**
     * Formats file size for display.
     *
     * @return Human-readable file size
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        }
        if (fileSize < 1024 * 1024) {
            return (fileSize / 1024) + " KB";
        }
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }

    /**
     * Gets file extension from original filename.
     *
     * @return File extension in lowercase
     */
    public String getFileExtension() {
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "";
        }
        return originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
