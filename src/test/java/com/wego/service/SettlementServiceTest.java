package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.domain.settlement.DebtSimplifier;
import com.wego.domain.settlement.Settlement;
import com.wego.dto.response.SettlementResponse;
import com.wego.entity.Expense;
import com.wego.entity.ExpenseSplit;
import com.wego.entity.Role;
import com.wego.entity.SplitType;
import com.wego.entity.Trip;
import com.wego.entity.TripMember;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ResourceNotFoundException;
import com.wego.repository.ExpenseRepository;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import com.wego.dto.response.ExchangeRateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SettlementService.
 *
 * @contract
 *   - Tests follow TDD methodology
 *   - Covers all public methods
 *   - Tests edge cases and error scenarios
 */
@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

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

    @Mock
    private DebtSimplifier debtSimplifier;

    @Mock
    private ExchangeRateService exchangeRateService;

    private SettlementService settlementService;

    private UUID tripId;
    private UUID userId;
    private UUID user2Id;
    private UUID user3Id;
    private UUID expenseId;
    private UUID splitId;
    private User testUser;
    private User testUser2;
    private User testUser3;
    private Trip testTrip;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();
        user2Id = UUID.randomUUID();
        user3Id = UUID.randomUUID();
        expenseId = UUID.randomUUID();
        splitId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("user1@example.com")
                .nickname("User 1")
                .build();

        testUser2 = User.builder()
                .id(user2Id)
                .email("user2@example.com")
                .nickname("User 2")
                .build();

        testUser3 = User.builder()
                .id(user3Id)
                .email("user3@example.com")
                .nickname("User 3")
                .build();

        testTrip = Trip.builder()
                .id(tripId)
                .title("Test Trip")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .baseCurrency("TWD")
                .ownerId(userId)
                .build();

        // Manually create SettlementService with all dependencies
        settlementService = new SettlementService(
                expenseRepository,
                expenseSplitRepository,
                tripRepository,
                userRepository,
                permissionChecker,
                debtSimplifier,
                exchangeRateService,
                null,
                new NoOpCacheManager()
        );
    }

    @Nested
    @DisplayName("calculateSettlement")
    class CalculateSettlementTests {

        @Test
        @DisplayName("should calculate settlement correctly for simple case")
        void calculateSettlement_withSimpleCase_shouldReturnCorrectSettlements() {
            // Given
            // User1 paid 1000, split equally between User1 and User2
            // User2 owes User1 500
            Expense expense = Expense.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            ExpenseSplit split1 = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expenseId)
                    .userId(userId)
                    .amount(new BigDecimal("500"))
                    .isSettled(false)
                    .build();

            ExpenseSplit split2 = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expenseId)
                    .userId(user2Id)
                    .amount(new BigDecimal("500"))
                    .isSettled(false)
                    .build();

            Settlement settlement = Settlement.builder()
                    .fromUserId(user2Id)
                    .toUserId(userId)
                    .amount(new BigDecimal("500"))
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.singletonList(expense));
            when(expenseSplitRepository.findByTripId(tripId))
                    .thenReturn(Arrays.asList(split1, split2));
            when(debtSimplifier.simplify(anyMap())).thenReturn(Collections.singletonList(settlement));
            when(userRepository.findAllById(any())).thenReturn(Arrays.asList(testUser, testUser2));

            // When
            SettlementResponse response = settlementService.calculateSettlement(tripId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(response.getBaseCurrency()).isEqualTo("TWD");
            assertThat(response.getSettlements()).hasSize(1);
            assertThat(response.getSettlements().get(0).getFromUserId()).isEqualTo(user2Id);
            assertThat(response.getSettlements().get(0).getToUserId()).isEqualTo(userId);
            assertThat(response.getSettlements().get(0).getAmount())
                    .isEqualByComparingTo(new BigDecimal("500"));
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no view permission")
        void calculateSettlement_withNoPermission_shouldThrowForbiddenException() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> settlementService.calculateSettlement(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when trip not found")
        void calculateSettlement_withNonExistentTrip_shouldThrowResourceNotFoundException() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> settlementService.calculateSettlement(tripId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should return empty settlements when no expenses exist")
        void calculateSettlement_withNoExpenses_shouldReturnEmptySettlements() {
            // Given
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.emptyList());
            when(expenseSplitRepository.findByTripId(tripId))
                    .thenReturn(Collections.emptyList());
            when(debtSimplifier.simplify(anyMap())).thenReturn(Collections.emptyList());

            // When
            SettlementResponse response = settlementService.calculateSettlement(tripId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getSettlements()).isEmpty();
        }

        @Test
        @DisplayName("should handle complex multi-person settlement")
        void calculateSettlement_withMultiplePeople_shouldReturnSimplifiedSettlements() {
            // Given
            // User1 paid 1200 (split 3 ways: 400 each)
            // User2 paid 600 (split 3 ways: 200 each)
            // Expected balances: User1: +800, User2: +200, User3: -1000
            Expense expense1 = Expense.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .amount(new BigDecimal("1200"))
                    .paidBy(userId)
                    .build();

            Expense expense2 = Expense.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .amount(new BigDecimal("600"))
                    .paidBy(user2Id)
                    .build();

            // Splits for expense1
            ExpenseSplit split1_1 = ExpenseSplit.builder()
                    .expenseId(expense1.getId())
                    .userId(userId)
                    .amount(new BigDecimal("400"))
                    .build();
            ExpenseSplit split1_2 = ExpenseSplit.builder()
                    .expenseId(expense1.getId())
                    .userId(user2Id)
                    .amount(new BigDecimal("400"))
                    .build();
            ExpenseSplit split1_3 = ExpenseSplit.builder()
                    .expenseId(expense1.getId())
                    .userId(user3Id)
                    .amount(new BigDecimal("400"))
                    .build();

            // Splits for expense2
            ExpenseSplit split2_1 = ExpenseSplit.builder()
                    .expenseId(expense2.getId())
                    .userId(userId)
                    .amount(new BigDecimal("200"))
                    .build();
            ExpenseSplit split2_2 = ExpenseSplit.builder()
                    .expenseId(expense2.getId())
                    .userId(user2Id)
                    .amount(new BigDecimal("200"))
                    .build();
            ExpenseSplit split2_3 = ExpenseSplit.builder()
                    .expenseId(expense2.getId())
                    .userId(user3Id)
                    .amount(new BigDecimal("200"))
                    .build();

            Settlement settlement1 = Settlement.builder()
                    .fromUserId(user3Id)
                    .toUserId(userId)
                    .amount(new BigDecimal("600"))
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Arrays.asList(expense1, expense2));
            when(expenseSplitRepository.findByTripId(tripId))
                    .thenReturn(Arrays.asList(split1_1, split1_2, split1_3, split2_1, split2_2, split2_3));
            when(debtSimplifier.simplify(anyMap())).thenReturn(Collections.singletonList(settlement1));
            when(userRepository.findAllById(any())).thenReturn(Arrays.asList(testUser, testUser3));

            // When
            SettlementResponse response = settlementService.calculateSettlement(tripId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("1800"));
            assertThat(response.getExpenseCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should convert multi-currency expenses to base currency")
        void calculateSettlement_withMultipleCurrencies_shouldConvertToBaseCurrency() {
            // Given
            // User1 paid 100 USD (= 3150 TWD at rate 31.5)
            // User2 paid 10000 JPY (= 2100 TWD at rate 0.21)
            // Trip base currency is TWD
            // Total in TWD: 3150 + 2100 = 5250
            UUID expense1Id = UUID.randomUUID();
            UUID expense2Id = UUID.randomUUID();

            Expense usdExpense = Expense.builder()
                    .id(expense1Id)
                    .tripId(tripId)
                    .description("Hotel")
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            Expense jpyExpense = Expense.builder()
                    .id(expense2Id)
                    .tripId(tripId)
                    .description("Train")
                    .amount(new BigDecimal("10000"))
                    .currency("JPY")
                    .paidBy(user2Id)
                    .splitType(SplitType.EQUAL)
                    .build();

            // Splits - in original currency
            ExpenseSplit split1_1 = ExpenseSplit.builder()
                    .expenseId(expense1Id)
                    .userId(userId)
                    .amount(new BigDecimal("50.00"))
                    .build();
            ExpenseSplit split1_2 = ExpenseSplit.builder()
                    .expenseId(expense1Id)
                    .userId(user2Id)
                    .amount(new BigDecimal("50.00"))
                    .build();
            ExpenseSplit split2_1 = ExpenseSplit.builder()
                    .expenseId(expense2Id)
                    .userId(userId)
                    .amount(new BigDecimal("5000"))
                    .build();
            ExpenseSplit split2_2 = ExpenseSplit.builder()
                    .expenseId(expense2Id)
                    .userId(user2Id)
                    .amount(new BigDecimal("5000"))
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Arrays.asList(usdExpense, jpyExpense));
            when(expenseSplitRepository.findByTripId(tripId))
                    .thenReturn(Arrays.asList(split1_1, split1_2, split2_1, split2_2));

            // Mock exchange rate conversions
            when(exchangeRateService.convert(new BigDecimal("100.00"), "USD", "TWD"))
                    .thenReturn(new BigDecimal("3150.00"));
            when(exchangeRateService.convert(new BigDecimal("10000"), "JPY", "TWD"))
                    .thenReturn(new BigDecimal("2100.00"));
            when(exchangeRateService.convert(new BigDecimal("50.00"), "USD", "TWD"))
                    .thenReturn(new BigDecimal("1575.00"));
            when(exchangeRateService.convert(new BigDecimal("5000"), "JPY", "TWD"))
                    .thenReturn(new BigDecimal("1050.00"));

            // Expected settlements: User2 owes User1
            // User1: paid 3150, owes 1575 + 1050 = 2625, balance = +525
            // User2: paid 2100, owes 1575 + 1050 = 2625, balance = -525
            Settlement settlement = Settlement.builder()
                    .fromUserId(user2Id)
                    .toUserId(userId)
                    .amount(new BigDecimal("525.00"))
                    .build();

            when(debtSimplifier.simplify(anyMap())).thenReturn(Collections.singletonList(settlement));
            when(userRepository.findAllById(any())).thenReturn(Arrays.asList(testUser, testUser2));

            // When
            SettlementResponse response = settlementService.calculateSettlement(tripId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getBaseCurrency()).isEqualTo("TWD");
            // Total should be in base currency (TWD)
            assertThat(response.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("5250.00"));
            assertThat(response.getExpenseCount()).isEqualTo(2);

            // Currency breakdown should show original amounts
            assertThat(response.getCurrencyBreakdown()).isNotNull();
            assertThat(response.getCurrencyBreakdown()).hasSize(2);
            assertThat(response.getCurrencyBreakdown().get("USD")).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(response.getCurrencyBreakdown().get("JPY")).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("should have zero balances after all non-payer splits are settled")
        void calculateSettlement_withAllNonPayerSplitsSettled_shouldHaveZeroBalances() {
            // Given: User1 paid 1000, split equally with User2
            // User2's split is SETTLED, User1's self-split is UNSETTLED
            // Bug scenario: payer's unsettled self-split should NOT create phantom balance
            Expense expense = Expense.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            ExpenseSplit payerSplit = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expenseId)
                    .userId(userId)
                    .amount(new BigDecimal("500"))
                    .isSettled(false)
                    .build();

            ExpenseSplit settledSplit = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expenseId)
                    .userId(user2Id)
                    .amount(new BigDecimal("500"))
                    .isSettled(true)
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.singletonList(expense));
            when(expenseSplitRepository.findByTripId(tripId))
                    .thenReturn(Arrays.asList(payerSplit, settledSplit));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<UUID, BigDecimal>> balancesCaptor = ArgumentCaptor.forClass(Map.class);
            when(debtSimplifier.simplify(balancesCaptor.capture())).thenReturn(Collections.emptyList());

            // When
            SettlementResponse response = settlementService.calculateSettlement(tripId, userId);

            // Then: balances should be empty (no phantom positive balance for payer)
            Map<UUID, BigDecimal> capturedBalances = balancesCaptor.getValue();
            assertThat(capturedBalances).isEmpty();
            assertThat(response.getSettlements()).isEmpty();
        }

        @Test
        @DisplayName("should show correct balance when only some non-payer splits are settled")
        void calculateSettlement_withPartialSettlement_shouldShowCorrectBalance() {
            // Given: User1 paid 900, split equally among User1, User2, User3 (300 each)
            // User2's split is SETTLED, User3's is NOT
            UUID expense1Id = UUID.randomUUID();

            Expense expense = Expense.builder()
                    .id(expense1Id)
                    .tripId(tripId)
                    .description("Dinner")
                    .amount(new BigDecimal("900"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            ExpenseSplit payerSplit = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expense1Id)
                    .userId(userId)
                    .amount(new BigDecimal("300"))
                    .isSettled(false)
                    .build();

            ExpenseSplit settledSplit = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expense1Id)
                    .userId(user2Id)
                    .amount(new BigDecimal("300"))
                    .isSettled(true)
                    .build();

            ExpenseSplit unsettledSplit = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expense1Id)
                    .userId(user3Id)
                    .amount(new BigDecimal("300"))
                    .isSettled(false)
                    .build();

            Settlement expectedSettlement = Settlement.builder()
                    .fromUserId(user3Id)
                    .toUserId(userId)
                    .amount(new BigDecimal("300"))
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.singletonList(expense));
            when(expenseSplitRepository.findByTripId(tripId))
                    .thenReturn(Arrays.asList(payerSplit, settledSplit, unsettledSplit));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<UUID, BigDecimal>> balancesCaptor = ArgumentCaptor.forClass(Map.class);
            when(debtSimplifier.simplify(balancesCaptor.capture()))
                    .thenReturn(Collections.singletonList(expectedSettlement));
            when(userRepository.findAllById(any())).thenReturn(Arrays.asList(testUser, testUser3));

            // When
            settlementService.calculateSettlement(tripId, userId);

            // Then: payer credit = only user3's unsettled split (300)
            Map<UUID, BigDecimal> capturedBalances = balancesCaptor.getValue();
            assertThat(capturedBalances).hasSize(2);
            assertThat(capturedBalances.get(userId)).isEqualByComparingTo(new BigDecimal("300"));
            assertThat(capturedBalances.get(user3Id)).isEqualByComparingTo(new BigDecimal("-300"));
        }

        @Test
        @DisplayName("should handle expense where payer has no split (custom split)")
        void calculateSettlement_withPayerNotInSplits_shouldCreditPayerCorrectly() {
            // Given: User1 paid 600, custom split: User2=400, User3=200 (payer has no split)
            UUID expense1Id = UUID.randomUUID();

            Expense expense = Expense.builder()
                    .id(expense1Id)
                    .tripId(tripId)
                    .description("Gift")
                    .amount(new BigDecimal("600"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.CUSTOM)
                    .build();

            ExpenseSplit split2 = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expense1Id)
                    .userId(user2Id)
                    .amount(new BigDecimal("400"))
                    .isSettled(false)
                    .build();

            ExpenseSplit split3 = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expense1Id)
                    .userId(user3Id)
                    .amount(new BigDecimal("200"))
                    .isSettled(false)
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.singletonList(expense));
            when(expenseSplitRepository.findByTripId(tripId))
                    .thenReturn(Arrays.asList(split2, split3));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<UUID, BigDecimal>> balancesCaptor = ArgumentCaptor.forClass(Map.class);
            when(debtSimplifier.simplify(balancesCaptor.capture())).thenReturn(Collections.emptyList());

            // When
            settlementService.calculateSettlement(tripId, userId);

            // Then: payer credit = 400 + 200 = 600 (all splits belong to others)
            Map<UUID, BigDecimal> capturedBalances = balancesCaptor.getValue();
            assertThat(capturedBalances.get(userId)).isEqualByComparingTo(new BigDecimal("600"));
            assertThat(capturedBalances.get(user2Id)).isEqualByComparingTo(new BigDecimal("-400"));
            assertThat(capturedBalances.get(user3Id)).isEqualByComparingTo(new BigDecimal("-200"));
        }

        @Test
        @DisplayName("should return empty settlements when all splits are settled")
        void calculateSettlement_withAllSplitsSettled_shouldReturnNoSettlements() {
            // Given: User1 paid 1000, split with User2, BOTH splits settled
            Expense expense = Expense.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            ExpenseSplit payerSplit = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expenseId)
                    .userId(userId)
                    .amount(new BigDecimal("500"))
                    .isSettled(true)
                    .build();

            ExpenseSplit otherSplit = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expenseId)
                    .userId(user2Id)
                    .amount(new BigDecimal("500"))
                    .isSettled(true)
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.singletonList(expense));
            when(expenseSplitRepository.findByTripId(tripId))
                    .thenReturn(Arrays.asList(payerSplit, otherSplit));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<UUID, BigDecimal>> balancesCaptor = ArgumentCaptor.forClass(Map.class);
            when(debtSimplifier.simplify(balancesCaptor.capture())).thenReturn(Collections.emptyList());

            // When
            SettlementResponse response = settlementService.calculateSettlement(tripId, userId);

            // Then
            Map<UUID, BigDecimal> capturedBalances = balancesCaptor.getValue();
            assertThat(capturedBalances).isEmpty();
            assertThat(response.getSettlements()).isEmpty();
        }

        @Test
        @DisplayName("should skip conversion for same currency expenses")
        void calculateSettlement_withSameCurrency_shouldNotConvert() {
            // Given - all expenses in TWD (base currency)
            Expense expense = Expense.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .description("Dinner")
                    .amount(new BigDecimal("1000"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            ExpenseSplit split1 = ExpenseSplit.builder()
                    .expenseId(expenseId)
                    .userId(userId)
                    .amount(new BigDecimal("500"))
                    .build();
            ExpenseSplit split2 = ExpenseSplit.builder()
                    .expenseId(expenseId)
                    .userId(user2Id)
                    .amount(new BigDecimal("500"))
                    .build();

            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId))
                    .thenReturn(Collections.singletonList(expense));
            when(expenseSplitRepository.findByTripId(tripId))
                    .thenReturn(Arrays.asList(split1, split2));

            Settlement settlement = Settlement.builder()
                    .fromUserId(user2Id)
                    .toUserId(userId)
                    .amount(new BigDecimal("500"))
                    .build();

            when(debtSimplifier.simplify(anyMap())).thenReturn(Collections.singletonList(settlement));
            when(userRepository.findAllById(any())).thenReturn(Arrays.asList(testUser, testUser2));

            // When
            SettlementResponse response = settlementService.calculateSettlement(tripId, userId);

            // Then
            assertThat(response.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("1000"));
            assertThat(response.getCurrencyBreakdown()).hasSize(1);
            assertThat(response.getCurrencyBreakdown().get("TWD")).isEqualByComparingTo(new BigDecimal("1000"));
        }
    }

    @Nested
    @DisplayName("markAsSettled")
    class MarkAsSettledTests {

        @Test
        @DisplayName("should mark split as settled when user has permission")
        void markAsSettled_withValidPermission_shouldMarkSettled() {
            // Given
            ExpenseSplit split = ExpenseSplit.builder()
                    .id(splitId)
                    .expenseId(expenseId)
                    .userId(user2Id)
                    .amount(new BigDecimal("500"))
                    .isSettled(false)
                    .build();

            Expense expense = Expense.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .paidBy(userId)
                    .build();

            when(expenseSplitRepository.findById(splitId)).thenReturn(Optional.of(split));
            when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(expenseSplitRepository.save(any(ExpenseSplit.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            settlementService.markAsSettled(splitId, userId);

            // Then
            verify(expenseSplitRepository).save(any(ExpenseSplit.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when split not found")
        void markAsSettled_withNonExistentSplit_shouldThrowResourceNotFoundException() {
            // Given
            when(expenseSplitRepository.findById(splitId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> settlementService.markAsSettled(splitId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no permission")
        void markAsSettled_withNoPermission_shouldThrowForbiddenException() {
            // Given
            ExpenseSplit split = ExpenseSplit.builder()
                    .id(splitId)
                    .expenseId(expenseId)
                    .userId(user2Id)
                    .amount(new BigDecimal("500"))
                    .build();

            Expense expense = Expense.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .paidBy(userId)
                    .build();

            when(expenseSplitRepository.findById(splitId)).thenReturn(Optional.of(split));
            when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
            when(permissionChecker.canEdit(tripId, user3Id)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> settlementService.markAsSettled(splitId, user3Id))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("markAsUnsettled")
    class MarkAsUnsettledTests {

        @Test
        @DisplayName("should mark split as unsettled when user has permission")
        void markAsUnsettled_withValidPermission_shouldMarkUnsettled() {
            // Given
            ExpenseSplit split = ExpenseSplit.builder()
                    .id(splitId)
                    .expenseId(expenseId)
                    .userId(user2Id)
                    .amount(new BigDecimal("500"))
                    .isSettled(true)
                    .build();

            Expense expense = Expense.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .paidBy(userId)
                    .build();

            when(expenseSplitRepository.findById(splitId)).thenReturn(Optional.of(split));
            when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
            when(permissionChecker.canEdit(tripId, userId)).thenReturn(true);
            when(expenseSplitRepository.save(any(ExpenseSplit.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            settlementService.markAsUnsettled(splitId, userId);

            // Then
            verify(expenseSplitRepository).save(any(ExpenseSplit.class));
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when split not found")
        void markAsUnsettled_withNonExistentSplit_shouldThrowResourceNotFoundException() {
            // Given
            when(expenseSplitRepository.findById(splitId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> settlementService.markAsUnsettled(splitId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getUserBalance")
    class GetUserBalanceTests {

        @Test
        @DisplayName("should return positive balance when user is owed money (single currency)")
        void getUserBalance_singleCurrency_creditor() {
            // Given: User1 paid 1000 TWD, split equally with User2
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

            Expense expense = Expense.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .amount(new BigDecimal("1000"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            ExpenseSplit split = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expenseId)
                    .userId(user2Id)
                    .amount(new BigDecimal("500"))
                    .isSettled(false)
                    .build();

            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId)).thenReturn(List.of(expense));
            when(expenseSplitRepository.findByTripId(tripId)).thenReturn(List.of(split));

            // When
            BigDecimal balance = settlementService.getUserBalance(tripId, userId);

            // Then: User1 is owed 500
            assertThat(balance).isEqualByComparingTo(new BigDecimal("500"));
        }

        @Test
        @DisplayName("should return negative balance when user owes money (single currency)")
        void getUserBalance_singleCurrency_debtor() {
            // Given: User1 paid 1000 TWD, User2 owes 500
            when(permissionChecker.canView(tripId, user2Id)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

            Expense expense = Expense.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .amount(new BigDecimal("1000"))
                    .currency("TWD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            ExpenseSplit split = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expenseId)
                    .userId(user2Id)
                    .amount(new BigDecimal("500"))
                    .isSettled(false)
                    .build();

            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId)).thenReturn(List.of(expense));
            when(expenseSplitRepository.findByTripId(tripId)).thenReturn(List.of(split));

            // When
            BigDecimal balance = settlementService.getUserBalance(tripId, user2Id);

            // Then: User2 owes 500
            assertThat(balance).isEqualByComparingTo(new BigDecimal("-500"));
        }

        @Test
        @DisplayName("should convert multi-currency expenses to base currency")
        void getUserBalance_multiCurrency_shouldConvert() {
            // Given: User1 paid 100 USD, split equally with User2, base=TWD
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));

            Expense expense = Expense.builder()
                    .id(expenseId)
                    .tripId(tripId)
                    .amount(new BigDecimal("100"))
                    .currency("USD")
                    .paidBy(userId)
                    .splitType(SplitType.EQUAL)
                    .build();

            ExpenseSplit split = ExpenseSplit.builder()
                    .id(UUID.randomUUID())
                    .expenseId(expenseId)
                    .userId(user2Id)
                    .amount(new BigDecimal("50"))
                    .isSettled(false)
                    .build();

            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId)).thenReturn(List.of(expense));
            when(expenseSplitRepository.findByTripId(tripId)).thenReturn(List.of(split));
            // USD 50 → TWD 1600 (rate 32)
            when(exchangeRateService.convert(new BigDecimal("50"), "USD", "TWD"))
                    .thenReturn(new BigDecimal("1600.00"));

            // When
            BigDecimal balance = settlementService.getUserBalance(tripId, userId);

            // Then: User1 is owed 1600 TWD
            assertThat(balance).isEqualByComparingTo(new BigDecimal("1600.00"));
        }

        @Test
        @DisplayName("should return zero when user has no expenses")
        void getUserBalance_noExpenses_shouldReturnZero() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(testTrip));
            when(expenseRepository.findByTripIdOrderByCreatedAtDesc(tripId)).thenReturn(List.of());
            when(expenseSplitRepository.findByTripId(tripId)).thenReturn(List.of());

            BigDecimal balance = settlementService.getUserBalance(tripId, userId);

            assertThat(balance).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user has no view permission")
        void getUserBalance_noPermission_shouldThrow() {
            when(permissionChecker.canView(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> settlementService.getUserBalance(tripId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
