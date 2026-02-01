package com.wego.service;

import com.wego.dto.DocumentFilter;
import com.wego.dto.response.DocumentWithTripResponse;
import com.wego.dto.response.GlobalDocumentOverviewResponse;
import com.wego.dto.response.TripSummary;
import com.wego.entity.Document;
import com.wego.entity.Trip;
import com.wego.entity.TripMember;
import com.wego.entity.User;
import com.wego.repository.DocumentRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for global document operations across all trips.
 *
 * @contract
 *   - All methods filter by trips user is member of
 *   - calledBy: GlobalDocumentController
 *   - calls: DocumentRepository, TripMemberRepository, TripRepository, UserRepository
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalDocumentService {

    private final DocumentRepository documentRepository;
    private final TripMemberRepository tripMemberRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;

    /**
     * Gets the global document overview for a user.
     *
     * @contract
     *   - pre: userId != null
     *   - post: Returns paginated documents user has access to
     *   - calls: TripMemberRepository#findByUserId, DocumentRepository queries
     *   - calledBy: GlobalDocumentController#showDocumentOverview
     *
     * @param userId The user ID
     * @param filter Filter parameters
     * @param pageable Pagination parameters
     * @return Global document overview with pagination
     */
    public GlobalDocumentOverviewResponse getOverview(UUID userId,
                                                       DocumentFilter filter,
                                                       Pageable pageable) {
        // Get all accessible trip IDs
        List<UUID> tripIds = tripMemberRepository.findByUserId(userId).stream()
                .map(TripMember::getTripId)
                .collect(Collectors.toList());

        if (tripIds.isEmpty()) {
            log.debug("User {} has no trips, returning empty document overview", userId);
            return GlobalDocumentOverviewResponse.empty();
        }

        // Apply trip filter if specified, verify access
        List<UUID> queryTripIds;
        if (filter.getTripId() != null) {
            if (tripIds.contains(filter.getTripId())) {
                queryTripIds = List.of(filter.getTripId());
            } else {
                log.warn("User {} tried to filter by trip {} they don't have access to",
                        userId, filter.getTripId());
                return GlobalDocumentOverviewResponse.empty();
            }
        } else {
            queryTripIds = tripIds;
        }

        // Query with filters
        Page<Document> documentPage = documentRepository.findByFilters(
                queryTripIds,
                filter.getSearch(),
                filter.getMimeTypes(),
                pageable);

        // Calculate totals across all accessible trips (not filtered)
        long totalDocuments = documentRepository.countByTripIdIn(tripIds);
        long totalStorage = documentRepository.sumFileSizeByTripIdIn(tripIds);

        // Build response with trip and uploader info
        List<DocumentWithTripResponse> documents = mapDocuments(documentPage.getContent());

        log.debug("User {} document overview: {} documents, {} total storage",
                userId, totalDocuments, totalStorage);

        return GlobalDocumentOverviewResponse.builder()
                .documents(documents)
                .totalDocuments(totalDocuments)
                .totalStorageBytes(totalStorage)
                .currentPage(documentPage.getNumber())
                .totalPages(documentPage.getTotalPages())
                .hasNext(documentPage.hasNext())
                .hasPrevious(documentPage.hasPrevious())
                .build();
    }

    /**
     * Gets trips that have documents for filter dropdown.
     * Uses batch query to avoid N+1 problem.
     *
     * @contract
     *   - pre: userId != null
     *   - post: Returns trips with document count > 0
     *   - calledBy: GlobalDocumentController#showDocumentOverview
     *
     * @param userId The user ID
     * @return List of trip summaries with document counts
     */
    public List<TripSummary> getUserTripsWithDocuments(UUID userId) {
        List<UUID> tripIds = tripMemberRepository.findByUserId(userId).stream()
                .map(TripMember::getTripId)
                .collect(Collectors.toList());

        if (tripIds.isEmpty()) {
            return List.of();
        }

        // Batch query to get document counts - avoids N+1 problem
        Map<UUID, Long> countMap = documentRepository.countByTripIds(tripIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        return tripRepository.findAllById(tripIds).stream()
                .filter(trip -> countMap.getOrDefault(trip.getId(), 0L) > 0)
                .map(trip -> TripSummary.builder()
                        .id(trip.getId())
                        .title(trip.getTitle())
                        .documentCount(countMap.getOrDefault(trip.getId(), 0L))
                        .build())
                .sorted((a, b) -> Long.compare(b.getDocumentCount(), a.getDocumentCount()))
                .collect(Collectors.toList());
    }

    /**
     * Maps documents to response DTOs with trip and uploader info.
     *
     * @param documents List of documents
     * @return List of document responses with trip info
     */
    private List<DocumentWithTripResponse> mapDocuments(List<Document> documents) {
        if (documents.isEmpty()) {
            return List.of();
        }

        // Get trip and user info for all documents
        Set<UUID> tripIds = documents.stream()
                .map(Document::getTripId)
                .collect(Collectors.toSet());
        Set<UUID> uploaderIds = documents.stream()
                .map(Document::getUploadedBy)
                .collect(Collectors.toSet());

        Map<UUID, Trip> tripMap = tripRepository.findAllById(tripIds).stream()
                .collect(Collectors.toMap(Trip::getId, Function.identity()));
        Map<UUID, User> userMap = userRepository.findAllById(uploaderIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return documents.stream()
                .map(doc -> {
                    Trip trip = tripMap.get(doc.getTripId());
                    User uploader = userMap.get(doc.getUploadedBy());
                    return DocumentWithTripResponse.builder()
                            .id(doc.getId())
                            .fileName(doc.getFileName())
                            .originalFileName(doc.getOriginalFileName())
                            .fileUrl(doc.getFileUrl())
                            .fileSize(doc.getFileSize())
                            .mimeType(doc.getMimeType())
                            .isImage(doc.isImage())
                            .isPdf(doc.isPdf())
                            .tripId(doc.getTripId())
                            .tripTitle(trip != null ? trip.getTitle() : "Unknown")
                            .uploadedByName(uploader != null ? uploader.getNickname() : "Unknown")
                            .uploadedByAvatarUrl(uploader != null ? uploader.getAvatarUrl() : null)
                            .createdAt(doc.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
