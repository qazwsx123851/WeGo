package com.wego.domain.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

/**
 * Utility class for file content validation via magic bytes.
 * Consolidates validation logic previously duplicated in TripService and DocumentService.
 */
public final class FileValidationUtils {

    private static final Logger log = LoggerFactory.getLogger(FileValidationUtils.class);

    /**
     * Unified magic bytes map covering images (JPEG, PNG, WebP) and documents (PDF).
     */
    private static final Map<String, byte[][]> MAGIC_BYTES = Map.of(
            "application/pdf", new byte[][] { {0x25, 0x50, 0x44, 0x46} },
            "image/jpeg", new byte[][] {
                    {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0},
                    {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE1},
                    {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE8}
            },
            "image/png", new byte[][] {
                    {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
            },
            "image/webp", new byte[][] {
                    {0x52, 0x49, 0x46, 0x46}
            }
    );

    private FileValidationUtils() {
    }

    /**
     * Validates that a file's actual content matches its declared MIME type
     * by checking magic bytes in the file header.
     *
     * @param file         the uploaded file
     * @param declaredType the declared Content-Type
     * @return true if content matches or no validation is defined for the type
     */
    public static boolean matchesMagicBytes(MultipartFile file, String declaredType) {
        // Skip validation for HEIC (complex container format)
        if ("image/heic".equals(declaredType)) {
            return true;
        }

        byte[][] expectedSignatures = MAGIC_BYTES.get(declaredType);
        if (expectedSignatures == null) {
            return true;
        }

        try (InputStream is = file.getInputStream()) {
            int maxLength = Math.max(12, Arrays.stream(expectedSignatures)
                    .mapToInt(sig -> sig.length)
                    .max()
                    .orElse(8));
            byte[] header = new byte[maxLength];
            int bytesRead = is.read(header);

            if (bytesRead < 4) {
                return false;
            }

            for (byte[] signature : expectedSignatures) {
                if (bytesRead >= signature.length && startsWith(header, signature)) {
                    if ("image/webp".equals(declaredType)) {
                        return bytesRead >= 12
                                && header[8] == 'W' && header[9] == 'E'
                                && header[10] == 'B' && header[11] == 'P';
                    }
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            log.error("Failed to read file for magic bytes validation", e);
            return false;
        }
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
