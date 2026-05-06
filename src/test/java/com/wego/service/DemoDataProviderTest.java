package com.wego.service;

import com.wego.dto.response.ActivityResponse;
import com.wego.dto.response.ExpenseResponse;
import com.wego.dto.response.TripResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DemoDataProvider} detail accessor methods.
 *
 * <p>Demo data is deterministic and entirely in-memory; no mocks required.
 */
class DemoDataProviderTest {

    private DemoDataProvider demoDataProvider;

    @BeforeEach
    void setUp() {
        demoDataProvider = new DemoDataProvider();
    }

    @Nested
    @DisplayName("getActivity(UUID)")
    class GetActivityTests {

        @Test
        @DisplayName("returns activity when ID exists in getAllDemoActivities()")
        void returnsActivity_whenIdExists() {
            UUID existingId = demoDataProvider.getAllDemoActivities().get(0).getId();

            Optional<ActivityResponse> result = demoDataProvider.getActivity(existingId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(existingId);
        }

        @Test
        @DisplayName("returns empty when ID does not exist")
        void returnsEmpty_whenIdDoesNotExist() {
            UUID nonExistentId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

            Optional<ActivityResponse> result = demoDataProvider.getActivity(nonExistentId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when ID is null")
        void returnsEmpty_whenIdIsNull() {
            Optional<ActivityResponse> result = demoDataProvider.getActivity(null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getExpense(UUID)")
    class GetExpenseTests {

        @Test
        @DisplayName("returns expense when ID exists in getDemoExpenses()")
        void returnsExpense_whenIdExists() {
            UUID existingId = demoDataProvider.getDemoExpenses().get(0).getId();

            Optional<ExpenseResponse> result = demoDataProvider.getExpense(existingId);

            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(existingId);
        }

        @Test
        @DisplayName("returns empty when ID does not exist")
        void returnsEmpty_whenIdDoesNotExist() {
            UUID nonExistentId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

            Optional<ExpenseResponse> result = demoDataProvider.getExpense(nonExistentId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when ID is null")
        void returnsEmpty_whenIdIsNull() {
            Optional<ExpenseResponse> result = demoDataProvider.getExpense(null);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getMember(UUID)")
    class GetMemberTests {

        @Test
        @DisplayName("returns member when userId exists in getDemoTrip().getMembers()")
        void returnsMember_whenUserIdExists() {
            UUID existingUserId = demoDataProvider.getDemoTrip().getMembers().get(0).getUserId();

            Optional<TripResponse.MemberSummary> result = demoDataProvider.getMember(existingUserId);

            assertThat(result).isPresent();
            assertThat(result.get().getUserId()).isEqualTo(existingUserId);
        }

        @Test
        @DisplayName("returns empty when userId does not exist")
        void returnsEmpty_whenUserIdDoesNotExist() {
            UUID nonExistentId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

            Optional<TripResponse.MemberSummary> result = demoDataProvider.getMember(nonExistentId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns empty when userId is null")
        void returnsEmpty_whenIdIsNull() {
            Optional<TripResponse.MemberSummary> result = demoDataProvider.getMember(null);

            assertThat(result).isEmpty();
        }
    }
}
