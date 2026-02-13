package com.wego.service;

import com.wego.config.ChatProperties;
import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.response.ChatResponse;
import com.wego.entity.Activity;
import com.wego.entity.Place;
import com.wego.entity.Trip;
import com.wego.exception.BusinessException;
import com.wego.exception.ForbiddenException;
import com.wego.repository.ActivityRepository;
import com.wego.repository.PlaceRepository;
import com.wego.repository.TripRepository;
import com.wego.service.external.GeminiClient;
import com.wego.service.external.GeminiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService")
class ChatServiceTest {

    @Mock
    private GeminiClient geminiClient;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private ActivityRepository activityRepository;
    @Mock
    private PlaceRepository placeRepository;
    @Mock
    private PermissionChecker permissionChecker;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private ChatProperties chatProperties;

    @InjectMocks
    private ChatService chatService;

    private UUID tripId;
    private UUID userId;
    private Trip trip;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();

        trip = Trip.builder()
                .id(tripId)
                .title("東京五日遊")
                .description("日本旅行")
                .startDate(LocalDate.now().minusDays(2))
                .endDate(LocalDate.now().plusDays(2))
                .ownerId(userId)
                .build();
    }

    @Nested
    @DisplayName("Permission checks")
    class PermissionChecks {

        @Test
        @DisplayName("should throw ForbiddenException when user is not a trip member")
        void shouldThrowWhenNotMember() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> chatService.chat(tripId, userId, "test"))
                    .isInstanceOf(ForbiddenException.class);

            verify(geminiClient, never()).chat(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Rate limiting")
    class RateLimiting {

        @Test
        @DisplayName("should throw BusinessException when rate limited")
        void shouldThrowWhenRateLimited() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(chatProperties.getRateLimitPerMinute()).thenReturn(5);
            when(rateLimitService.isAllowed(eq("chat:" + userId), eq(5))).thenReturn(false);

            assertThatThrownBy(() -> chatService.chat(tripId, userId, "test"))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getErrorCode()).isEqualTo("RATE_LIMITED"));

            verify(geminiClient, never()).chat(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Successful chat")
    class SuccessfulChat {

        @BeforeEach
        void setUp() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(chatProperties.getRateLimitPerMinute()).thenReturn(5);
            when(rateLimitService.isAllowed(anyString(), anyInt())).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
        }

        @Test
        @DisplayName("should return AI reply on successful chat")
        void shouldReturnReply() {
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(eq(tripId), anyInt()))
                    .thenReturn(List.of());
            when(geminiClient.chat(anyString(), eq("推薦餐廳")))
                    .thenReturn("推薦你去鼎泰豐！");

            ChatResponse response = chatService.chat(tripId, userId, "推薦餐廳");

            assertThat(response.getReply()).isEqualTo("推薦你去鼎泰豐！");
        }

        @Test
        @DisplayName("should include trip context in system prompt")
        void shouldIncludeTripContext() {
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(eq(tripId), anyInt()))
                    .thenReturn(List.of());
            when(geminiClient.chat(anyString(), anyString())).thenReturn("reply");

            chatService.chat(tripId, userId, "test");

            ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
            verify(geminiClient).chat(promptCaptor.capture(), eq("test"));

            String prompt = promptCaptor.getValue();
            assertThat(prompt).contains("東京五日遊");
            assertThat(prompt).contains("旅遊助手");
            assertThat(prompt).contains("範圍限制");
        }

        @Test
        @DisplayName("should include today's activities in prompt when within date range")
        void shouldIncludeTodayActivities() {
            UUID placeId = UUID.randomUUID();
            Activity activity = Activity.builder()
                    .tripId(tripId)
                    .day(3) // today is day 3 (startDate was 2 days ago)
                    .startTime(LocalTime.of(9, 0))
                    .durationMinutes(120)
                    .placeId(placeId)
                    .build();

            Place place = Place.builder()
                    .id(placeId)
                    .name("淺草寺")
                    .address("東京都台東區淺草2-3-1")
                    .latitude(35.7148)
                    .longitude(139.7967)
                    .build();

            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(tripId, 3))
                    .thenReturn(List.of(activity));
            when(placeRepository.findAllById(List.of(placeId)))
                    .thenReturn(List.of(place));
            when(geminiClient.chat(anyString(), anyString())).thenReturn("reply");

            chatService.chat(tripId, userId, "test");

            ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
            verify(geminiClient).chat(promptCaptor.capture(), anyString());

            String prompt = promptCaptor.getValue();
            assertThat(prompt).contains("淺草寺");
            assertThat(prompt).contains("09:00");
            assertThat(prompt).contains("120分鐘");
        }
    }

    @Nested
    @DisplayName("Trip context edge cases")
    class TripContextEdgeCases {

        @Test
        @DisplayName("should show not-in-range message when today is before trip start")
        void shouldShowNotInRangeBeforeStart() {
            Trip futureTrip = Trip.builder()
                    .id(tripId)
                    .title("未來旅行")
                    .startDate(LocalDate.now().plusDays(10))
                    .endDate(LocalDate.now().plusDays(15))
                    .ownerId(userId)
                    .build();

            String context = chatService.buildTripContext(futureTrip);

            assertThat(context).contains("不在行程日期範圍內");
            assertThat(context).doesNotContain("第");
        }

        @Test
        @DisplayName("should show not-in-range message when today is after trip end")
        void shouldShowNotInRangeAfterEnd() {
            Trip pastTrip = Trip.builder()
                    .id(tripId)
                    .title("過去旅行")
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now().minusDays(5))
                    .ownerId(userId)
                    .build();

            String context = chatService.buildTripContext(pastTrip);

            assertThat(context).contains("不在行程日期範圍內");
        }

        @Test
        @DisplayName("should show day number when today is within trip range")
        void shouldShowDayNumberWhenInRange() {
            String context = chatService.buildTripContext(trip);

            assertThat(context).contains("第 3 天"); // startDate was 2 days ago, so today is day 3
        }
    }

    @Nested
    @DisplayName("Gemini error handling")
    class GeminiErrorHandling {

        @BeforeEach
        void setUp() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(chatProperties.getRateLimitPerMinute()).thenReturn(5);
            when(rateLimitService.isAllowed(anyString(), anyInt())).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(trip));
            when(activityRepository.findByTripIdAndDayOrderBySortOrderAsc(eq(tripId), anyInt()))
                    .thenReturn(List.of());
        }

        @Test
        @DisplayName("should return friendly error message when Gemini fails")
        void shouldReturnFriendlyErrorOnGeminiFailure() {
            when(geminiClient.chat(anyString(), anyString()))
                    .thenThrow(GeminiException.apiError("API error"));

            ChatResponse response = chatService.chat(tripId, userId, "test");

            assertThat(response.getReply()).contains("暫時無法回覆");
        }

        @Test
        @DisplayName("should return friendly error message when Gemini times out")
        void shouldReturnFriendlyErrorOnTimeout() {
            when(geminiClient.chat(anyString(), anyString()))
                    .thenThrow(GeminiException.timeout());

            ChatResponse response = chatService.chat(tripId, userId, "test");

            assertThat(response.getReply()).contains("暫時無法回覆");
        }
    }
}
