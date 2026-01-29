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
