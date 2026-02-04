package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreateExpenseRequest;
import com.wego.dto.request.UpdateExpenseRequest;
import com.wego.dto.response.ExpenseResponse;
import com.wego.entity.Expense;
import com.wego.entity.ExpenseSplit;
import com.wego.entity.Role;
import com.wego.entity.SplitType;
import com.wego.entity.Trip;
import com.wego.entity.TripMember;
import com.wego.entity.User;
import com.wego.exception.BusinessException;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ExpenseService.
 *
 * @contract
 *   - Tests follow TDD methodology
 *   - Covers all public methods
 *   - Tests edge cases and error scenarios
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseSplitRepository expenseSplitRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionChecker permissionChecker;

    @InjectMocks
    private ExpenseService expenseService;

    private UUID tripId;
    private UUID userId;
    private UUID expenseId;
    private UUID splitId;
    private User testUser;
    private User testUser2;
    private Trip testTrip;
    private Expense testExpense;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();
        expenseId = UUID.randomUUID();
        splitId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .nickname("Test User")
                .provider("google")
                .providerId("123456")
                .build();

        testUser2 = User.builder()
                .id(UUID.randomUUID())
                .email("test2@example.com")
                .nickname("Test User 2")
                .provider("google")
                .providerId("654321")
                .build();

        testTrip = Trip.builder()
                .id(tripId)
                .title("Test Trip")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .baseCurrency("TWD")
                .ownerId(userId)
                .build();

        testExpense = Expense.builder()
                .id(expenseId)
                .tripId(tripId)
                .description("Test Expense")
                .amount(new BigDecimal("1000"))
                .currency("TWD")
                .paidBy(userId)
                .splitType(SplitType.EQUAL)
                .createdBy(userId)
                .build();
    }

    @Nested
    @DisplayName("createExpense")
    class CreateExpenseTests {

        @Test
        @DisplayName("should create expense with equal split when valid input")
        void createExpense_withValidInput_shouldReturnCreatedExpense() {
            // Given
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .category("Food")
                    .expenseDate(LocalDate.now())
                    .build();

            TripMember member1 = TripMember.builder()
                    .tripId(tripId)
                    .userId(userId)
                    .role(Role.OWNER)
                    .build();
            TripMember member2 = TripMember.builder()
                    .tripId(tripId)
                    .userId(testUser2.getId())
                    .role(Role.EDITOR)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(Arrays.asList(member1, member2));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(testUser2.getId())).thenReturn(Optional.of(testUser2));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(expenseId);
                return expense;
            });
            when(expenseSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(expenseSplitRepository.findByExpenseId(expenseId)).thenReturn(Collections.emptyList());
            when(userRepository.findAllById(any())).thenReturn(List.of());

            // When
            ExpenseResponse response = expenseService.createExpense(tripId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDescription()).isEqualTo("Dinner");
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(response.getPaidBy()).isEqualTo(userId);
            assertThat(response.getPaidByName()).isEqualTo("Test User");

            verify(expenseRepository).save(any(Expense.class));
            verify(expenseSplitRepository).saveAll(any());
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no edit permission")
        void createExpense_withNoPermission_shouldThrowForbiddenException() {
            // Given
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> expenseService.createExpense(tripId, request, userId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("permission");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when trip not found")
        void createExpense_withNonExistentTrip_shouldThrowResourceNotFoundException() {
            // Given
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> expenseService.createExpense(tripId, request, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Trip");
        }

        @Test
        @DisplayName("should create expense with custom splits")
        void createExpense_withCustomSplits_shouldCreateCorrectSplits() {
            // Given
            CreateExpenseRequest.SplitRequest split1 = CreateExpenseRequest.SplitRequest.builder()
                    .userId(userId)
                    .amount(new BigDecimal("600"))
                    .build();
            CreateExpenseRequest.SplitRequest split2 = CreateExpenseRequest.SplitRequest.builder()
                    .userId(testUser2.getId())
                    .amount(new BigDecimal("400"))
                    .build();

            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.CUSTOM)
                    .splits(Arrays.asList(split1, split2))
                    .build();

            TripMember member1 = TripMember.builder()
                    .tripId(tripId)
                    .userId(userId)
                    .role(Role.OWNER)
                    .build();
            TripMember member2 = TripMember.builder()
                    .tripId(tripId)
                    .userId(testUser2.getId())
                    .role(Role.EDITOR)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(Arrays.asList(member1, member2));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(expenseId);
                return expense;
            });
            when(expenseSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(expenseSplitRepository.findByExpenseId(expenseId)).thenReturn(Collections.emptyList());
            when(userRepository.findAllById(any())).thenReturn(List.of());

            // When
            ExpenseResponse response = expenseService.createExpense(tripId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getSplitType()).isEqualTo(SplitType.CUSTOM);

            ArgumentCaptor<List<ExpenseSplit>> splitsCaptor = ArgumentCaptor.forClass(List.class);
            verify(expenseSplitRepository).saveAll(splitsCaptor.capture());

            List<ExpenseSplit> savedSplits = splitsCaptor.getValue();
            assertThat(savedSplits).hasSize(2);
        }

        @Test
        @DisplayName("should throw BusinessException when custom split amounts do not match total")
        void createExpense_withInvalidCustomSplits_shouldThrowBusinessException() {
            // Given
            CreateExpenseRequest.SplitRequest split1 = CreateExpenseRequest.SplitRequest.builder()
                    .userId(userId)
                    .amount(new BigDecimal("500"))
                    .build();
            CreateExpenseRequest.SplitRequest split2 = CreateExpenseRequest.SplitRequest.builder()
                    .userId(testUser2.getId())
                    .amount(new BigDecimal("300"))
                    .build();

            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .paidBy(userId)
                    .splitType(SplitType.CUSTOM)
                    .splits(Arrays.asList(split1, split2))
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

            // When & Then
            assertThatThrownBy(() -> expenseService.createExpense(tripId, request, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContainingAll("Split", "amount");
        }
    }

    @Nested
    @DisplayName("getExpensesByTrip")
    class GetExpensesByTripTests {

        @Test
        @DisplayName("should return expenses for trip when user has view permission")
        void getExpensesByTrip_withValidPermission_shouldReturnExpenses() {
            // Given
            ExpenseSplit split = ExpenseSplit.builder()
                    .id(splitId)
                    .expenseId(expenseId)
                    .userId(userId)
                    .amount(new BigDecimal("500"))
                    .isSettled(false)
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.singletonList(testExpense));
            when(expenseSplitRepository.findByExpenseId(expenseId))
                    .thenReturn(Collections.singletonList(split));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.findAllById(any())).thenReturn(List.of(testUser));

            // When
            List<ExpenseResponse> responses = expenseService.getExpensesByTrip(tripId, userId);

            // Then
            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getDescription()).isEqualTo("Test Expense");
            assertThat(responses.get(0).getPaidByName()).isEqualTo("Test User");
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no view permission")
        void getExpensesByTrip_withNoPermission_shouldThrowForbiddenException() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> expenseService.getExpensesByTrip(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should return empty list when no expenses exist")
        void getExpensesByTrip_withNoExpenses_shouldReturnEmptyList() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.emptyList());

            // When
            List<ExpenseResponse> responses = expenseService.getExpensesByTrip(tripId, userId);

            // Then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateExpense")
    class UpdateExpenseTests {

        @Test
        @DisplayName("should update expense when user has edit permission")
        void updateExpense_withValidInput_shouldReturnUpdatedExpense() {
            // Given
            UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                    .description("Updated Dinner")
                    .amount(new BigDecimal("1500"))
                    .build();

            when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(expenseSplitRepository.findByExpenseId(expenseId)).thenReturn(Collections.emptyList());
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(Collections.emptyList());
            when(userRepository.findAllById(any())).thenReturn(List.of());

            // When
            ExpenseResponse response = expenseService.updateExpense(expenseId, request, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDescription()).isEqualTo("Updated Dinner");
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("1500"));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when expense not found")
        void updateExpense_withNonExistentExpense_shouldThrowResourceNotFoundException() {
            // Given
            UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                    .description("Updated")
                    .build();

            when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> expenseService.updateExpense(expenseId, request, userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Expense");
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no edit permission")
        void updateExpense_withNoPermission_shouldThrowForbiddenException() {
            // Given
            UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                    .description("Updated")
                    .build();

            when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> expenseService.updateExpense(expenseId, request, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should only update provided fields")
        void updateExpense_withPartialUpdate_shouldOnlyUpdateProvidedFields() {
            // Given
            UpdateExpenseRequest request = UpdateExpenseRequest.builder()
                    .category("Restaurant")
                    .build();

            when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(expenseSplitRepository.findByExpenseId(expenseId)).thenReturn(Collections.emptyList());
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(Collections.emptyList());
            when(userRepository.findAllById(any())).thenReturn(List.of());

            // When
            ExpenseResponse response = expenseService.updateExpense(expenseId, request, userId);

            // Then
            assertThat(response.getDescription()).isEqualTo("Test Expense"); // unchanged
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("1000")); // unchanged
            assertThat(response.getCategory()).isEqualTo("Restaurant"); // updated
        }
    }

    @Nested
    @DisplayName("deleteExpense")
    class DeleteExpenseTests {

        @Test
        @DisplayName("should delete expense when user is creator")
        void deleteExpense_byCreator_shouldDeleteSuccessfully() {
            // Given
            when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);

            // When
            expenseService.deleteExpense(expenseId, userId);

            // Then
            verify(expenseSplitRepository).deleteByExpenseId(expenseId);
            verify(expenseRepository).delete(testExpense);
        }

        @Test
        @DisplayName("should delete expense when user is trip owner")
        void deleteExpense_byTripOwner_shouldDeleteSuccessfully() {
            // Given
            UUID ownerId = UUID.randomUUID();
            UUID creatorId = UUID.randomUUID();
            Expense otherUserExpense = Expense.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .description("Other's Expense")
                    .amount(new BigDecimal("500"))
                    .paidBy(creatorId)
                    .createdBy(creatorId)
                    .build();

            when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(otherUserExpense));
            when(permissionChecker.canEdit(tripId, ownerId)).thenReturn(true);
            when(permissionChecker.canDelete(tripId, ownerId)).thenReturn(true);

            // When
            expenseService.deleteExpense(expenseId, ownerId);

            // Then
            verify(expenseSplitRepository).deleteByExpenseId(expenseId);
            verify(expenseRepository).delete(otherUserExpense);
        }

        @Test
        @DisplayName("should throw ForbiddenException when non-creator non-owner tries to delete")
        void deleteExpense_byOtherUser_shouldThrowForbiddenException() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(testExpense));
            when(permissionChecker.canEdit(tripId, otherUserId)).thenReturn(true);
            when(permissionChecker.canDelete(tripId, otherUserId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> expenseService.deleteExpense(expenseId, otherUserId))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("delete");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when expense not found")
        void deleteExpense_withNonExistentExpense_shouldThrowResourceNotFoundException() {
            // Given
            when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> expenseService.deleteExpense(expenseId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("should handle null splits gracefully for EQUAL split type")
        void createExpense_withNullSplitsForEqual_shouldSucceed() {
            // Given
            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .splits(null)
                    .build();

            TripMember member = TripMember.builder()
                    .tripId(tripId)
                    .userId(userId)
                    .role(Role.OWNER)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(Collections.singletonList(member));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(expenseId);
                return expense;
            });
            when(expenseSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(expenseSplitRepository.findByExpenseId(expenseId)).thenReturn(Collections.emptyList());
            when(userRepository.findAllById(any())).thenReturn(List.of());

            // When
            ExpenseResponse response = expenseService.createExpense(tripId, request, userId);

            // Then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("should handle zero amount split for CUSTOM type")
        void createExpense_withZeroAmountSplit_shouldSucceed() {
            // Given
            CreateExpenseRequest.SplitRequest split1 = CreateExpenseRequest.SplitRequest.builder()
                    .userId(userId)
                    .amount(new BigDecimal("1000"))
                    .build();
            CreateExpenseRequest.SplitRequest split2 = CreateExpenseRequest.SplitRequest.builder()
                    .userId(testUser2.getId())
                    .amount(BigDecimal.ZERO)
                    .build();

            CreateExpenseRequest request = CreateExpenseRequest.builder()
                    .description("Gift")
                    .amount(new BigDecimal("1000"))
                    .paidBy(userId)
                    .splitType(SplitType.CUSTOM)
                    .splits(Arrays.asList(split1, split2))
                    .build();

            TripMember member1 = TripMember.builder()
                    .tripId(tripId)
                    .userId(userId)
                    .role(Role.OWNER)
                    .build();
            TripMember member2 = TripMember.builder()
                    .tripId(tripId)
                    .userId(testUser2.getId())
                    .role(Role.EDITOR)
                    .build();

            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(tripMemberRepository.findByTripId(tripId)).thenReturn(Arrays.asList(member1, member2));
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense expense = invocation.getArgument(0);
                expense.setId(expenseId);
                return expense;
            });
            when(expenseSplitRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(expenseSplitRepository.findByExpenseId(expenseId)).thenReturn(Collections.emptyList());
            when(userRepository.findAllById(any())).thenReturn(List.of());

            // When
            ExpenseResponse response = expenseService.createExpense(tripId, request, userId);

            // Then
            assertThat(response).isNotNull();
        }
    }
}
