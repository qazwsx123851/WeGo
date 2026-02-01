package com.wego.repository;

import com.wego.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for Document entity operations.
 *
 * @contract
 *   - pre: All parameters must be non-null unless marked Optional
 *   - calledBy: DocumentService
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Finds all documents for a trip.
     *
     * @param tripId The trip ID
     * @return List of documents
     */
    List<Document> findByTripIdOrderByCreatedAtDesc(UUID tripId);

    /**
     * Finds documents linked to a specific activity.
     *
     * @param relatedActivityId The activity ID
     * @return List of documents
     */
    List<Document> findByRelatedActivityId(UUID relatedActivityId);

    /**
     * Finds documents linked to a specific day.
     *
     * @param tripId The trip ID
     * @param relatedDay The day number
     * @return List of documents
     */
    List<Document> findByTripIdAndRelatedDay(UUID tripId, Integer relatedDay);

    /**
     * Calculates total file size for a trip.
     *
     * @param tripId The trip ID
     * @return Total file size in bytes
     */
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.tripId = :tripId")
    long sumFileSizeByTripId(@Param("tripId") UUID tripId);

    /**
     * Counts documents in a trip.
     *
     * @param tripId The trip ID
     * @return Number of documents
     */
    long countByTripId(UUID tripId);

    /**
     * Counts documents uploaded by a user.
     *
     * @param uploadedBy The user ID
     * @return Number of documents
     */
    long countByUploadedBy(UUID uploadedBy);

    /**
     * Deletes all documents for a trip.
     *
     * @param tripId The trip ID
     */
    void deleteByTripId(UUID tripId);

    /**
     * Clears activity association for documents linked to an activity.
     * Used when an activity is deleted.
     *
     * @param activityId The activity ID
     */
    @Modifying
    @Query("UPDATE Document d SET d.relatedActivityId = null WHERE d.relatedActivityId = :activityId")
    void clearActivityAssociation(@Param("activityId") UUID activityId);

    // ========== Global Document Methods ==========

    /**
     * Finds documents with filters across multiple trips.
     *
     * @contract
     *   - pre: tripIds not empty
     *   - post: Returns paginated documents matching filters
     *   - calledBy: GlobalDocumentService#getOverview
     *
     * @param tripIds List of trip IDs user has access to
     * @param search Search term for filename/description (nullable)
     * @param mimeTypes List of MIME types to filter (nullable for all)
     * @param pageable Pagination parameters
     * @return Page of documents
     */
    @Query("SELECT d FROM Document d WHERE d.tripId IN :tripIds " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(d.originalFileName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:mimeTypes IS NULL OR d.mimeType IN :mimeTypes) " +
           "ORDER BY d.createdAt DESC")
    Page<Document> findByFilters(@Param("tripIds") List<UUID> tripIds,
                                  @Param("search") String search,
                                  @Param("mimeTypes") List<String> mimeTypes,
                                  Pageable pageable);

    /**
     * Counts documents across multiple trips.
     *
     * @contract
     *   - pre: tripIds not empty
     *   - post: Returns count >= 0
     *   - calledBy: GlobalDocumentService#getOverview
     *
     * @param tripIds List of trip IDs
     * @return Total document count
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.tripId IN :tripIds")
    long countByTripIdIn(@Param("tripIds") List<UUID> tripIds);

    /**
     * Sums file sizes across multiple trips.
     *
     * @contract
     *   - pre: tripIds not empty
     *   - post: Returns sum of file sizes, 0 if none
     *   - calledBy: GlobalDocumentService#getOverview
     *
     * @param tripIds List of trip IDs
     * @return Total file size in bytes
     */
    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM Document d WHERE d.tripId IN :tripIds")
    long sumFileSizeByTripIdIn(@Param("tripIds") List<UUID> tripIds);

    /**
     * Counts documents by trip ID for multiple trips in a single query.
     * Avoids N+1 query problem.
     *
     * @contract
     *   - pre: tripIds not empty
     *   - post: Returns list of [tripId, count] pairs
     *   - calledBy: GlobalDocumentService#getUserTripsWithDocuments
     *
     * @param tripIds List of trip IDs
     * @return List of Object arrays [tripId, count]
     */
    @Query("SELECT d.tripId, COUNT(d) FROM Document d WHERE d.tripId IN :tripIds GROUP BY d.tripId")
    List<Object[]> countByTripIds(@Param("tripIds") List<UUID> tripIds);
}
