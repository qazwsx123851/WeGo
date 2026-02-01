package com.wego.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for global document overview across all trips.
 *
 * @contract
 *   - documents: list of documents with trip info
 *   - totalDocuments: total count across all trips
 *   - totalStorageBytes: total storage used in bytes
 *   - pagination fields for UI
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalDocumentOverviewResponse {

    private List<DocumentWithTripResponse> documents;
    private long totalDocuments;
    private long totalStorageBytes;
    private int currentPage;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    /**
     * Creates an empty response for users with no documents.
     *
     * @return Empty GlobalDocumentOverviewResponse
     */
    public static GlobalDocumentOverviewResponse empty() {
        return GlobalDocumentOverviewResponse.builder()
                .documents(List.of())
                .totalDocuments(0)
                .totalStorageBytes(0)
                .currentPage(0)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    /**
     * Formats storage size for display.
     *
     * @return Human-readable storage size
     */
    public String getFormattedStorageSize() {
        if (totalStorageBytes < 1024) {
            return totalStorageBytes + " B";
        }
        if (totalStorageBytes < 1024 * 1024) {
            return (totalStorageBytes / 1024) + " KB";
        }
        return String.format("%.1f MB", totalStorageBytes / (1024.0 * 1024.0));
    }
}
