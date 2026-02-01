package com.wego.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Filter parameters for document queries.
 *
 * @contract
 *   - search: search in filename and description (nullable)
 *   - tripId: filter by specific trip (nullable)
 *   - fileType: "image", "pdf", or "all" (nullable = all)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFilter {

    private String search;
    private UUID tripId;
    private String fileType;

    /**
     * Converts file type filter to MIME type list.
     *
     * @return List of MIME types, or null for all types
     */
    public List<String> getMimeTypes() {
        if (fileType == null || "all".equals(fileType)) {
            return null;
        }
        if ("image".equals(fileType)) {
            return List.of("image/jpeg", "image/png", "image/gif", "image/webp", "image/heic");
        }
        if ("pdf".equals(fileType)) {
            return List.of("application/pdf");
        }
        return null;
    }

    /**
     * Checks if any filter is active.
     *
     * @return true if at least one filter is set
     */
    public boolean hasFilters() {
        return (search != null && !search.isBlank())
            || tripId != null
            || (fileType != null && !"all".equals(fileType));
    }
}
