package com.wego.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Document entity representing an uploaded file associated with a trip.
 *
 * Documents can be linked to a specific activity or day within the trip.
 * Files are stored in Supabase Storage, with metadata stored here.
 *
 * @contract
 *   - invariant: tripId is never null
 *   - invariant: fileName is never null or empty
 *   - invariant: fileUrl is never null or empty
 *   - invariant: uploadedBy is never null
 *   - invariant: fileSize >= 0
 *
 * @see Trip
 * @see Activity
 */
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_document_trip_id", columnList = "trip_id"),
    @Index(name = "idx_document_uploaded_by", columnList = "uploaded_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    /**
     * Maximum file size in bytes (10 MB)
     */
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Maximum total storage per trip in bytes (100 MB)
     */
    public static final long MAX_TRIP_STORAGE = 100 * 1024 * 1024;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "trip_id", nullable = false)
    private UUID tripId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "related_activity_id")
    private UUID relatedActivityId;

    @Column(name = "related_day")
    private Integer relatedDay;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "category", length = 50)
    @Builder.Default
    private String category = "other";

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Gets the file extension from the original file name.
     *
     * @return The file extension (without dot), or empty string if none
     */
    public String getFileExtension() {
        if (originalFileName == null || !originalFileName.contains(".")) {
            return "";
        }
        return originalFileName.substring(originalFileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Checks if this is an image file.
     *
     * @return true if the file is an image
     */
    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Checks if this is a PDF file.
     *
     * @return true if the file is a PDF
     */
    public boolean isPdf() {
        return "application/pdf".equals(mimeType);
    }

    /**
     * Equality based on ID for JPA entity identity.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return id != null && Objects.equals(id, document.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Document{" +
                "id=" + id +
                ", tripId=" + tripId +
                ", originalFileName='" + originalFileName + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }
}
