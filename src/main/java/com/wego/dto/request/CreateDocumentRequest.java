package com.wego.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a document upload.
 *
 * @contract
 *   - relatedActivityId: optional, links document to a specific activity
 *   - relatedDay: optional, links document to a specific day (1-based)
 *   - description: optional, max 500 characters
 *
 * @see com.wego.entity.Document
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentRequest {

    /**
     * Optional activity to link this document to.
     */
    private UUID relatedActivityId;

    /**
     * Optional day number within the trip (1-based index).
     */
    private Integer relatedDay;

    /**
     * Optional description of the document.
     */
    @Size(max = 500, message = "描述不可超過 500 字")
    private String description;
}
