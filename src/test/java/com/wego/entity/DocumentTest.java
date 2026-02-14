package com.wego.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Document entity.
 */
@Tag("fast")
@DisplayName("Document Entity Tests")
class DocumentTest {

    @Nested
    @DisplayName("File Extension")
    class FileExtension {

        @Test
        @DisplayName("Should extract file extension")
        void getFileExtension_withExtension_shouldReturnExtension() {
            Document document = new Document();
            document.setOriginalFileName("document.PDF");

            assertEquals("pdf", document.getFileExtension());
        }

        @Test
        @DisplayName("Should return empty for no extension")
        void getFileExtension_withoutExtension_shouldReturnEmpty() {
            Document document = new Document();
            document.setOriginalFileName("document");

            assertEquals("", document.getFileExtension());
        }

        @Test
        @DisplayName("Should handle null filename")
        void getFileExtension_withNullFilename_shouldReturnEmpty() {
            Document document = new Document();

            assertEquals("", document.getFileExtension());
        }

        @Test
        @DisplayName("Should handle multiple dots in filename")
        void getFileExtension_withMultipleDots_shouldReturnLastExtension() {
            Document document = new Document();
            document.setOriginalFileName("file.backup.tar.gz");

            assertEquals("gz", document.getFileExtension());
        }
    }

    @Nested
    @DisplayName("File Type Detection")
    class FileTypeDetection {

        @Test
        @DisplayName("Should detect image files")
        void isImage_withImageMimeType_shouldReturnTrue() {
            Document document = new Document();
            document.setMimeType("image/jpeg");

            assertTrue(document.isImage());
        }

        @Test
        @DisplayName("Should detect PNG as image")
        void isImage_withPngMimeType_shouldReturnTrue() {
            Document document = new Document();
            document.setMimeType("image/png");

            assertTrue(document.isImage());
        }

        @Test
        @DisplayName("Should not detect PDF as image")
        void isImage_withPdfMimeType_shouldReturnFalse() {
            Document document = new Document();
            document.setMimeType("application/pdf");

            assertFalse(document.isImage());
        }

        @Test
        @DisplayName("Should detect PDF files")
        void isPdf_withPdfMimeType_shouldReturnTrue() {
            Document document = new Document();
            document.setMimeType("application/pdf");

            assertTrue(document.isPdf());
        }

        @Test
        @DisplayName("Should not detect image as PDF")
        void isPdf_withImageMimeType_shouldReturnFalse() {
            Document document = new Document();
            document.setMimeType("image/jpeg");

            assertFalse(document.isPdf());
        }
    }

    @Nested
    @DisplayName("File Size Constants")
    class FileSizeConstants {

        @Test
        @DisplayName("MAX_FILE_SIZE should be 10 MB")
        void maxFileSize_shouldBe10MB() {
            assertEquals(10 * 1024 * 1024, Document.MAX_FILE_SIZE);
        }

        @Test
        @DisplayName("MAX_TRIP_STORAGE should be 100 MB")
        void maxTripStorage_shouldBe100MB() {
            assertEquals(100 * 1024 * 1024, Document.MAX_TRIP_STORAGE);
        }
    }

}
