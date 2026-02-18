package com.wego.service;

import com.wego.dto.DocumentFilter;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests for GlobalDocumentService.
 *
 * @contract
 *   - Tests global document overview across all trips
 *   - Tests document filter by trip ID and file type
 *   - Tests getUserTripsWithDocuments
 *   - Verifies access control (only trips user is member of)
 */
@ExtendWith(MockitoExtension.class)
class GlobalDocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GlobalDocumentService globalDocumentService;

    private UUID userId;
    private UUID tripId1;
    private UUID tripId2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tripId1 = UUID.randomUUID();
        tripId2 = UUID.randomUUID();
        pageable = PageRequest.of(0, 20);
    }

    private TripMember createMember(UUID tripId) {
        return TripMember.builder()
                .userId(userId)
                .tripId(tripId)
                .build();
    }

    @Nested
    @DisplayName("getOverview")
    class GetOverviewTests {

        @Test
        @DisplayName("should return empty overview when user has no trips")
        void getOverview_noTrips_shouldReturnEmpty() {
            when(tripMemberRepository.findByUserId(userId)).thenReturn(List.of());

            GlobalDocumentOverviewResponse result = globalDocumentService.getOverview(
                    userId, DocumentFilter.builder().build(), pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalDocuments()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return paginated documents for user trips")
        void getOverview_withDocuments_shouldReturnPaginated() {
            UUID uploaderId = UUID.randomUUID();
            Document doc = Document.builder()
                    .id(UUID.randomUUID())
                    .fileName("test.pdf")
                    .originalFileName("test.pdf")
                    .fileUrl("https://storage.example.com/test.pdf")
                    .fileSize(1024L)
                    .mimeType("application/pdf")
                    .tripId(tripId1)
                    .uploadedBy(uploaderId)
                    .createdAt(Instant.now())
                    .build();

            when(tripMemberRepository.findByUserId(userId))
                    .thenReturn(List.of(createMember(tripId1)));
            Page<Document> page = new PageImpl<>(List.of(doc), pageable, 1);
            when(documentRepository.findByFilters(any(), any(), any(), eq(pageable)))
                    .thenReturn(page);
            when(documentRepository.countByTripIdIn(any())).thenReturn(1L);
            when(documentRepository.sumFileSizeByTripIdIn(any())).thenReturn(1024L);

            Trip trip = Trip.builder().id(tripId1).title("Trip A").build();
            User uploader = User.builder().id(uploaderId).nickname("Uploader").build();
            when(tripRepository.findAllById(any())).thenReturn(List.of(trip));
            when(userRepository.findAllById(any())).thenReturn(List.of(uploader));

            GlobalDocumentOverviewResponse result = globalDocumentService.getOverview(
                    userId, DocumentFilter.builder().build(), pageable);

            assertThat(result.getTotalDocuments()).isEqualTo(1L);
            assertThat(result.getTotalStorageBytes()).isEqualTo(1024L);
            assertThat(result.getDocuments()).hasSize(1);
            assertThat(result.getDocuments().get(0).getTripTitle()).isEqualTo("Trip A");
            assertThat(result.getDocuments().get(0).getUploadedByName()).isEqualTo("Uploader");
        }

        @Test
        @DisplayName("should filter by tripId when specified and user has access")
        void getOverview_filterByTrip_shouldFilterCorrectly() {
            when(tripMemberRepository.findByUserId(userId))
                    .thenReturn(List.of(createMember(tripId1), createMember(tripId2)));
            Page<Document> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(documentRepository.findByFilters(eq(List.of(tripId1)), any(), any(), eq(pageable)))
                    .thenReturn(emptyPage);
            when(documentRepository.countByTripIdIn(any())).thenReturn(0L);
            when(documentRepository.sumFileSizeByTripIdIn(any())).thenReturn(0L);

            DocumentFilter filter = DocumentFilter.builder().tripId(tripId1).build();
            GlobalDocumentOverviewResponse result = globalDocumentService.getOverview(
                    userId, filter, pageable);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("should return empty when filtering by trip user has no access to")
        void getOverview_filterByInaccessibleTrip_shouldReturnEmpty() {
            UUID inaccessibleTripId = UUID.randomUUID();
            when(tripMemberRepository.findByUserId(userId))
                    .thenReturn(List.of(createMember(tripId1)));

            DocumentFilter filter = DocumentFilter.builder().tripId(inaccessibleTripId).build();
            GlobalDocumentOverviewResponse result = globalDocumentService.getOverview(
                    userId, filter, pageable);

            assertThat(result.getTotalDocuments()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getUserTripsWithDocuments")
    class GetUserTripsWithDocumentsTests {

        @Test
        @DisplayName("should return empty list when user has no trips")
        void getUserTripsWithDocuments_noTrips_shouldReturnEmpty() {
            when(tripMemberRepository.findByUserId(userId)).thenReturn(List.of());

            List<TripSummary> result = globalDocumentService.getUserTripsWithDocuments(userId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should return only trips with documents, sorted by count")
        void getUserTripsWithDocuments_withData_shouldReturnSortedByCount() {
            when(tripMemberRepository.findByUserId(userId))
                    .thenReturn(List.of(createMember(tripId1), createMember(tripId2)));

            // Trip1: 3 docs, Trip2: 5 docs
            List<Object[]> counts = Arrays.asList(
                    new Object[]{tripId1, 3L},
                    new Object[]{tripId2, 5L}
            );
            when(documentRepository.countByTripIds(any())).thenReturn(counts);

            Trip trip1 = Trip.builder().id(tripId1).title("Trip A").build();
            Trip trip2 = Trip.builder().id(tripId2).title("Trip B").build();
            when(tripRepository.findAllById(any())).thenReturn(List.of(trip1, trip2));

            List<TripSummary> result = globalDocumentService.getUserTripsWithDocuments(userId);

            assertThat(result).hasSize(2);
            // Trip B has 5 docs > Trip A has 3 docs
            assertThat(result.get(0).getTitle()).isEqualTo("Trip B");
            assertThat(result.get(0).getDocumentCount()).isEqualTo(5L);
            assertThat(result.get(1).getTitle()).isEqualTo("Trip A");
            assertThat(result.get(1).getDocumentCount()).isEqualTo(3L);
        }

        @Test
        @DisplayName("should exclude trips with zero documents")
        void getUserTripsWithDocuments_zeroDocTrips_shouldExclude() {
            when(tripMemberRepository.findByUserId(userId))
                    .thenReturn(List.of(createMember(tripId1), createMember(tripId2)));

            // Only Trip1 has documents
            List<Object[]> counts = Collections.singletonList(new Object[]{tripId1, 2L});
            when(documentRepository.countByTripIds(any())).thenReturn(counts);

            Trip trip1 = Trip.builder().id(tripId1).title("Trip A").build();
            Trip trip2 = Trip.builder().id(tripId2).title("Trip B").build();
            when(tripRepository.findAllById(any())).thenReturn(List.of(trip1, trip2));

            List<TripSummary> result = globalDocumentService.getUserTripsWithDocuments(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Trip A");
        }
    }
}
