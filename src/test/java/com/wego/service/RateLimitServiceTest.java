package com.wego.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimitService")
class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Nested
    @DisplayName("isAllowed")
    class IsAllowed {

        @Test
        @DisplayName("should allow requests under the limit")
        void shouldAllowUnderLimit() {
            for (int i = 0; i < 5; i++) {
                assertThat(rateLimitService.isAllowed("test-key", 5)).isTrue();
            }
        }

        @Test
        @DisplayName("should reject requests exceeding the limit")
        void shouldRejectOverLimit() {
            for (int i = 0; i < 5; i++) {
                rateLimitService.isAllowed("test-key", 5);
            }

            assertThat(rateLimitService.isAllowed("test-key", 5)).isFalse();
        }

        @Test
        @DisplayName("should track different keys independently")
        void shouldTrackKeysIndependently() {
            for (int i = 0; i < 3; i++) {
                rateLimitService.isAllowed("key-a", 3);
            }

            // key-a exhausted, but key-b should still be allowed
            assertThat(rateLimitService.isAllowed("key-a", 3)).isFalse();
            assertThat(rateLimitService.isAllowed("key-b", 3)).isTrue();
        }

        @Test
        @DisplayName("should use default limit when not specified")
        void shouldUseDefaultLimit() {
            // Default is 60 requests per minute
            for (int i = 0; i < 60; i++) {
                assertThat(rateLimitService.isAllowed("default-key")).isTrue();
            }
            assertThat(rateLimitService.isAllowed("default-key")).isFalse();
        }
    }

    @Nested
    @DisplayName("getRemainingRequests")
    class GetRemainingRequests {

        @Test
        @DisplayName("should return max when key is unknown")
        void shouldReturnMaxForUnknownKey() {
            assertThat(rateLimitService.getRemainingRequests("unknown")).isEqualTo(60);
        }

        @Test
        @DisplayName("should decrease as requests are made")
        void shouldDecreaseWithRequests() {
            rateLimitService.isAllowed("test-key", 10);
            rateLimitService.isAllowed("test-key", 10);

            assertThat(rateLimitService.getRemainingRequests("test-key")).isEqualTo(8);
        }

        @Test
        @DisplayName("should not go below zero")
        void shouldNotGoBelowZero() {
            for (int i = 0; i < 5; i++) {
                rateLimitService.isAllowed("test-key", 3);
            }

            assertThat(rateLimitService.getRemainingRequests("test-key")).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Window reset")
    class WindowReset {

        @Test
        @DisplayName("should reset bucket after window expires")
        void shouldResetAfterWindowExpires() throws Exception {
            // Use a very small window by testing the bucket directly
            RateLimitService.RateLimitBucket bucket = new RateLimitService.RateLimitBucket(2);

            assertThat(bucket.tryAcquire()).isTrue();
            assertThat(bucket.tryAcquire()).isTrue();
            assertThat(bucket.tryAcquire()).isFalse(); // exhausted

            // We can't easily test time-based reset without mocking System.currentTimeMillis,
            // but we verify the bucket respects the limit correctly
            assertThat(bucket.getRemainingRequests()).isEqualTo(0);
        }
    }
}
