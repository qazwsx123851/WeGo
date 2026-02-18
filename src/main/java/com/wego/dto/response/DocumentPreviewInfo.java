package com.wego.dto.response;

import java.util.UUID;

/**
 * DTO for document preview metadata.
 * Used to transfer document info from Service to Controller without exposing the Entity.
 */
public record DocumentPreviewInfo(
        UUID tripId,
        String fileName,
        String originalFileName,
        String mimeType
) {
}
