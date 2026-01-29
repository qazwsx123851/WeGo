package com.wego.repository;

import com.wego.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
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
    @Query("UPDATE Document d SET d.relatedActivityId = null WHERE d.relatedActivityId = :activityId")
    void clearActivityAssociation(@Param("activityId") UUID activityId);
}
