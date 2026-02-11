package com.wego.dto.response;

import com.wego.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for document information.
 *
 * @contract
 *   - id: the document ID
 *   - tripId: the trip this document belongs to
 *   - fileName: stored file name (UUID-based)
 *   - originalFileName: original uploaded file name
 *   - fileUrl: Supabase Storage URL
 *   - fileSize: file size in bytes
 *   - mimeType: MIME type of the file
 *
 * @see Document
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private UUID id;
    private UUID tripId;
    private String fileName;
    private String originalFileName;
    private String fileUrl;
    private long fileSize;
    private String mimeType;
    private String category;
    private UUID relatedActivityId;
    private Integer relatedDay;
    private String description;
    private UUID uploadedBy;
    private String uploadedByName;
    private String uploadedByAvatarUrl;
    private Instant createdAt;

    /**
     * Whether this document is an image file.
     */
    private boolean isImage;

    /**
     * Whether this document is a PDF file.
     */
    private boolean isPdf;

    /**
     * File extension (lowercase, without dot).
     */
    private String fileExtension;

    /**
     * Thumbnail URL for image files (same as fileUrl for images, null otherwise).
     */
    public String getThumbnailUrl() {
        return isImage ? fileUrl : null;
    }

    /**
     * Human-readable formatted file size.
     */
    public String getFormattedSize() {
        if (fileSize == 0) return "0 Bytes";
        String[] units = {"Bytes", "KB", "MB", "GB"};
        int i = (int) Math.floor(Math.log(fileSize) / Math.log(1024));
        i = Math.min(i, units.length - 1);
        double size = fileSize / Math.pow(1024, i);
        return String.format("%.1f %s", size, units[i]);
    }

    /**
     * Alias for createdAt (template uses uploadedAt).
     */
    public Instant getUploadedAt() {
        return createdAt;
    }

    /**
     * Alias for uploadedByName (template uses uploaderName).
     */
    public String getUploaderName() {
        return uploadedByName;
    }

    /**
     * Alias for uploadedByAvatarUrl (template uses uploaderAvatarUrl).
     */
    public String getUploaderAvatarUrl() {
        return uploadedByAvatarUrl;
    }

    /**
     * First character of uploader name for avatar placeholder.
     */
    public String getUploaderInitial() {
        if (uploadedByName != null && !uploadedByName.isEmpty()) {
            return uploadedByName.substring(0, 1);
        }
        return "?";
    }

    /**
     * Creates a DocumentResponse from a Document entity.
     * Note: uploadedByName and uploadedByAvatarUrl must be set separately.
     *
     * @param document The document entity
     * @return DocumentResponse DTO
     */
    public static DocumentResponse fromEntity(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .tripId(document.getTripId())
                .fileName(document.getFileName())
                .originalFileName(document.getOriginalFileName())
                .fileUrl(document.getFileUrl())
                .fileSize(document.getFileSize())
                .mimeType(document.getMimeType())
                .category(document.getCategory() != null ? document.getCategory() : "other")
                .relatedActivityId(document.getRelatedActivityId())
                .relatedDay(document.getRelatedDay())
                .description(document.getDescription())
                .uploadedBy(document.getUploadedBy())
                .createdAt(document.getCreatedAt())
                .isImage(document.isImage())
                .isPdf(document.isPdf())
                .fileExtension(document.getFileExtension())
                .build();
    }
}
