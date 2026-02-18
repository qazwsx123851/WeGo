package com.wego.service;

import com.wego.domain.permission.PermissionChecker;
import com.wego.dto.request.CreatePersonalExpenseRequest;
import com.wego.dto.request.SetPersonalBudgetRequest;
import com.wego.dto.request.UpdatePersonalExpenseRequest;
import com.wego.dto.response.PersonalExpenseItemResponse;
import com.wego.dto.response.PersonalExpenseItemResponse.Source;
import com.wego.dto.response.PersonalExpenseSummaryResponse;
import com.wego.dto.response.PersonalExpenseSummaryResponse.BudgetStatus;
import com.wego.entity.PersonalExpense;
import com.wego.entity.Role;
import com.wego.entity.Trip;
import com.wego.entity.TripMember;
import com.wego.entity.User;
import com.wego.exception.ForbiddenException;
import com.wego.exception.ValidationException;
import com.wego.dto.response.ExchangeRateResponse;
import com.wego.repository.ExpenseSplitRepository;
import com.wego.repository.ExpenseSplitRepository.AutoSplitProjection;
import com.wego.repository.PersonalExpenseRepository;
import com.wego.repository.TripMemberRepository;
import com.wego.repository.TripRepository;
import com.wego.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalExpenseServiceTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @Mock
    private ExpenseSplitRepository expenseSplitRepository;

    @Mock
    private PersonalExpenseRepository personalExpenseRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMemberRepository tripMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionChecker permissionChecker;

    @InjectMocks
    private PersonalExpenseService personalExpenseService;

    private UUID tripId;
    private UUID userId;
    private UUID payerUserId;
    private Trip mockTrip;

    @BeforeEach
    void setUp() {
        tripId = UUID.randomUUID();
        userId = UUID.randomUUID();
        payerUserId = UUID.randomUUID();

        mockTrip = Trip.builder()
                .id(tripId)
                .title("Test Trip")
                .startDate(LocalDate.of(2024, 3, 1))
                .endDate(LocalDate.of(2024, 3, 31))
                .baseCurrency("TWD")
                .build();
    }

    // ========== getPersonalExpenses ==========

    @Nested
    @DisplayName("getPersonalExpenses")
    class GetPersonalExpensesTests {

        @Test
        @DisplayName("should merge AUTO and MANUAL items, sorted by expenseDate ASC nulls last")
        void getPersonalExpenses_mergeAndSort_correctOrder() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));

            // AUTO item: Day 3 (2024-03-15)
            AutoSplitProjection autoSplit = mock(AutoSplitProjection.class);
            when(autoSplit.getAmount()).thenReturn(new BigDecimal("200.00"));
            when(autoSplit.getTripExpenseId()).thenReturn(UUID.randomUUID());
            when(autoSplit.getDescription()).thenReturn("Hotel");
            when(autoSplit.getCurrency()).thenReturn("TWD");
            when(autoSplit.getExchangeRate()).thenReturn(null);
            when(autoSplit.getCategory()).thenReturn("ACCOMMODATION");
            when(autoSplit.getExpenseDate()).thenReturn(LocalDate.of(2024, 3, 15));
            when(autoSplit.getPaidBy()).thenReturn(payerUserId);

            // MANUAL item: Day 1 (2024-03-01)
            PersonalExpense manualDay1 = PersonalExpense.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .tripId(tripId)
                    .description("Breakfast")
                    .amount(new BigDecimal("100.00"))
                    .currency("TWD")
                    .expenseDate(LocalDate.of(2024, 3, 1))
                    .build();

            // MANUAL item: null expenseDate
            PersonalExpense manualNullDate = PersonalExpense.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .tripId(tripId)
                    .description("Misc")
                    .amount(new BigDecimal("50.00"))
                    .currency("TWD")
                    .expenseDate(null)
                    .build();

            User payer = User.builder()
                    .id(payerUserId)
                    .nickname("Alice")
                    .email("alice@example.com")
                    .providerId("alice-provider-id")
                    .build();

            when(expenseSplitRepository.findPersonalSplitsByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(autoSplit));
            when(userRepository.findAllById(anyIterable())).thenReturn(List.of(payer));
            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(manualDay1, manualNullDate));

            List<PersonalExpenseItemResponse> result =
                    personalExpenseService.getPersonalExpenses(userId, tripId);

            assertThat(result).hasSize(3);
            // Day1 (2024-03-01) first
            assertThat(result.get(0).getExpenseDate()).isEqualTo(LocalDate.of(2024, 3, 1));
            assertThat(result.get(0).getSource()).isEqualTo(Source.MANUAL);
            // Day3 (2024-03-15) second
            assertThat(result.get(1).getExpenseDate()).isEqualTo(LocalDate.of(2024, 3, 15));
            assertThat(result.get(1).getSource()).isEqualTo(Source.AUTO);
            // null date last
            assertThat(result.get(2).getExpenseDate()).isNull();
            assertThat(result.get(2).getSource()).isEqualTo(Source.MANUAL);
        }

        @Test
        @DisplayName("should throw ForbiddenException when user is not a trip member")
        void getPersonalExpenses_nonMember_throwsForbiddenException() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(false);

            assertThatThrownBy(() -> personalExpenseService.getPersonalExpenses(userId, tripId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("foreign currency with null exchangeRate fetches rate on-the-fly")
        void getPersonalExpenses_foreignCurrencyNullRate_fetchesRateOnTheFly() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
            when(exchangeRateService.getRate("JPY", "TWD"))
                    .thenReturn(ExchangeRateResponse.fresh("JPY", "TWD",
                            new BigDecimal("0.2049"), java.time.Instant.now()));

            AutoSplitProjection jpySplit = mock(AutoSplitProjection.class);
            when(jpySplit.getAmount()).thenReturn(new BigDecimal("10000.00"));
            when(jpySplit.getCurrency()).thenReturn("JPY");
            when(jpySplit.getExchangeRate()).thenReturn(null);
            when(jpySplit.getDescription()).thenReturn("JPY expense");
            when(jpySplit.getExpenseDate()).thenReturn(LocalDate.of(2024, 3, 5));
            when(jpySplit.getPaidBy()).thenReturn(payerUserId);
            when(jpySplit.getTripExpenseId()).thenReturn(UUID.randomUUID());

            User payer = User.builder().id(payerUserId).nickname("Alice")
                    .email("a@test.com").providerId("p1").build();

            when(expenseSplitRepository.findPersonalSplitsByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(jpySplit));
            when(userRepository.findAllById(anyIterable())).thenReturn(List.of(payer));
            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of());

            List<PersonalExpenseItemResponse> result =
                    personalExpenseService.getPersonalExpenses(userId, tripId);

            assertThat(result).hasSize(1);
            // 10000 * 0.2049 = 2049.00
            assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("2049.00"));
            verify(exchangeRateService).getRate("JPY", "TWD");
        }

        @Test
        @DisplayName("foreign currency with null rate and API failure uses original amount")
        void getPersonalExpenses_foreignCurrencyApiFails_usesOriginalAmount() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
            when(exchangeRateService.getRate("JPY", "TWD"))
                    .thenThrow(new RuntimeException("API unavailable"));

            PersonalExpense manual = PersonalExpense.builder()
                    .id(UUID.randomUUID()).userId(userId).tripId(tripId)
                    .description("JPY manual").amount(new BigDecimal("5000.00"))
                    .currency("JPY").exchangeRate(null)
                    .expenseDate(LocalDate.of(2024, 3, 5)).build();

            when(expenseSplitRepository.findPersonalSplitsByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of());
            when(userRepository.findAllById(anyIterable())).thenReturn(List.of());
            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(manual));

            List<PersonalExpenseItemResponse> result =
                    personalExpenseService.getPersonalExpenses(userId, tripId);

            assertThat(result).hasSize(1);
            // Fallback: original amount used as-is
            assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        }

        @Test
        @DisplayName("same currency with null exchangeRate does NOT call exchange service")
        void getPersonalExpenses_sameCurrencyNullRate_noApiFetch() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));

            PersonalExpense twdExpense = PersonalExpense.builder()
                    .id(UUID.randomUUID()).userId(userId).tripId(tripId)
                    .description("TWD item").amount(new BigDecimal("100.00"))
                    .currency("TWD").exchangeRate(null)
                    .expenseDate(LocalDate.of(2024, 3, 5)).build();

            when(expenseSplitRepository.findPersonalSplitsByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of());
            when(userRepository.findAllById(anyIterable())).thenReturn(List.of());
            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(twdExpense));

            List<PersonalExpenseItemResponse> result =
                    personalExpenseService.getPersonalExpenses(userId, tripId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            verify(exchangeRateService, never()).getRate(any(), any());
        }

        @Test
        @DisplayName("should set AUTO source fields correctly")
        void getPersonalExpenses_autoItem_hasCorrectFields() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));

            UUID tripExpenseId = UUID.randomUUID();
            AutoSplitProjection autoSplit = mock(AutoSplitProjection.class);
            when(autoSplit.getAmount()).thenReturn(new BigDecimal("300.00"));
            when(autoSplit.getTripExpenseId()).thenReturn(tripExpenseId);
            when(autoSplit.getDescription()).thenReturn("Dinner");
            when(autoSplit.getCurrency()).thenReturn("TWD");
            when(autoSplit.getExchangeRate()).thenReturn(null);
            when(autoSplit.getCategory()).thenReturn("FOOD");
            when(autoSplit.getExpenseDate()).thenReturn(LocalDate.of(2024, 3, 10));
            when(autoSplit.getPaidBy()).thenReturn(payerUserId);

            User payer = User.builder()
                    .id(payerUserId)
                    .nickname("Bob")
                    .email("bob@example.com")
                    .providerId("bob-provider-id")
                    .build();

            when(expenseSplitRepository.findPersonalSplitsByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(autoSplit));
            when(userRepository.findAllById(anyIterable())).thenReturn(List.of(payer));
            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of());

            List<PersonalExpenseItemResponse> result =
                    personalExpenseService.getPersonalExpenses(userId, tripId);

            assertThat(result).hasSize(1);
            PersonalExpenseItemResponse item = result.get(0);
            assertThat(item.getSource()).isEqualTo(Source.AUTO);
            assertThat(item.getId()).isNull();
            assertThat(item.getTripExpenseId()).isEqualTo(tripExpenseId);
            assertThat(item.getPaidByName()).isEqualTo("Bob");
        }
    }

    // ========== getPersonalSummary ==========

    @Nested
    @DisplayName("getPersonalSummary")
    class GetPersonalSummaryTests {

        private void setupMemberAndTrip() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
            when(expenseSplitRepository.findPersonalSplitsByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of());
            when(userRepository.findAllById(anyIterable())).thenReturn(List.of());
        }

        @Test
        @DisplayName("exchangeRate null means amount unchanged (1:1 conversion)")
        void getPersonalSummary_nullExchangeRate_amountUnchanged() {
            setupMemberAndTrip();

            PersonalExpense expense = PersonalExpense.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .tripId(tripId)
                    .description("Coffee")
                    .amount(new BigDecimal("100.00"))
                    .currency("TWD")
                    .exchangeRate(null)
                    .expenseDate(LocalDate.of(2024, 3, 5))
                    .build();

            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(expense));
            when(tripMemberRepository.findByTripIdAndUserId(tripId, userId))
                    .thenReturn(Optional.empty());

            PersonalExpenseSummaryResponse result =
                    personalExpenseService.getPersonalSummary(userId, tripId);

            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("exchangeRate with value applies multiplication (100 * 0.5 = 50.00)")
        void getPersonalSummary_withExchangeRate_appliesMultiplication() {
            setupMemberAndTrip();

            PersonalExpense expense = PersonalExpense.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .tripId(tripId)
                    .description("Foreign item")
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .exchangeRate(new BigDecimal("0.5"))
                    .expenseDate(LocalDate.of(2024, 3, 5))
                    .build();

            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(expense));
            when(tripMemberRepository.findByTripIdAndUserId(tripId, userId))
                    .thenReturn(Optional.empty());

            PersonalExpenseSummaryResponse result =
                    personalExpenseService.getPersonalSummary(userId, tripId);

            assertThat(result.getTotalAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        }

        @Test
        @DisplayName("dailyAmounts includes zero-value entries for all trip dates")
        void getPersonalSummary_dailyAmounts_zeroFillsAllTripDates() {
            // Use a shorter trip range for a manageable assertion
            Trip shortTrip = Trip.builder()
                    .id(tripId)
                    .title("Short Trip")
                    .startDate(LocalDate.of(2024, 3, 1))
                    .endDate(LocalDate.of(2024, 3, 3))
                    .baseCurrency("TWD")
                    .build();

            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(shortTrip));
            when(expenseSplitRepository.findPersonalSplitsByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of());
            when(userRepository.findAllById(anyIterable())).thenReturn(List.of());

            // Expense only on day 1 — days 2 and 3 should still be present with zero
            PersonalExpense expense = PersonalExpense.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .tripId(tripId)
                    .description("Taxi")
                    .amount(new BigDecimal("80.00"))
                    .currency("TWD")
                    .expenseDate(LocalDate.of(2024, 3, 1))
                    .build();

            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(expense));
            when(tripMemberRepository.findByTripIdAndUserId(tripId, userId))
                    .thenReturn(Optional.empty());

            PersonalExpenseSummaryResponse result =
                    personalExpenseService.getPersonalSummary(userId, tripId);

            assertThat(result.getDailyAmounts()).containsKey(LocalDate.of(2024, 3, 1));
            assertThat(result.getDailyAmounts()).containsKey(LocalDate.of(2024, 3, 2));
            assertThat(result.getDailyAmounts()).containsKey(LocalDate.of(2024, 3, 3));
            assertThat(result.getDailyAmounts().get(LocalDate.of(2024, 3, 2)))
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getDailyAmounts().get(LocalDate.of(2024, 3, 3)))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("budget null returns BudgetStatus.NONE")
        void getPersonalSummary_nullBudget_returnsNone() {
            setupMemberAndTrip();
            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of());
            when(tripMemberRepository.findByTripIdAndUserId(tripId, userId))
                    .thenReturn(Optional.empty());

            PersonalExpenseSummaryResponse result =
                    personalExpenseService.getPersonalSummary(userId, tripId);

            assertThat(result.getBudgetStatus()).isEqualTo(BudgetStatus.NONE);
            assertThat(result.getBudget()).isNull();
        }

        @Test
        @DisplayName("budgetOverage is positive when RED (totalAmount - budget)")
        void getPersonalSummary_red_budgetOverageIsPositive() {
            setupMemberAndTrip();

            PersonalExpense expense = PersonalExpense.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .tripId(tripId)
                    .description("Overspent item")
                    .amount(new BigDecimal("1100.00"))
                    .currency("TWD")
                    .expenseDate(LocalDate.of(2024, 3, 5))
                    .build();

            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(expense));

            TripMember member = TripMember.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .userId(userId)
                    .role(Role.OWNER)
                    .personalBudget(new BigDecimal("1000.00"))
                    .build();
            when(tripMemberRepository.findByTripIdAndUserId(tripId, userId))
                    .thenReturn(Optional.of(member));

            PersonalExpenseSummaryResponse result =
                    personalExpenseService.getPersonalSummary(userId, tripId);

            assertThat(result.getBudgetStatus()).isEqualTo(BudgetStatus.RED);
            assertThat(result.getBudgetOverage()).isNotNull();
            assertThat(result.getBudgetOverage()).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @ParameterizedTest(name = "total={0}, budget={1} → {2}")
        @DisplayName("budgetStatus computed correctly based on spend ratio")
        @CsvSource({
            "799,1000,GREEN",
            "700,1000,GREEN",
            "800,1000,YELLOW",
            "850,1000,YELLOW",
            "999,1000,YELLOW",
            "1000,1000,RED",
            "1100,1000,RED"
        })
        void getPersonalSummary_budgetStatus_computedCorrectly(
                String totalStr, String budgetStr, String expectedStatus) {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
            when(expenseSplitRepository.findPersonalSplitsByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of());
            when(userRepository.findAllById(anyIterable())).thenReturn(List.of());

            BigDecimal total = new BigDecimal(totalStr);
            BigDecimal budget = new BigDecimal(budgetStr);

            PersonalExpense expense = PersonalExpense.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .tripId(tripId)
                    .description("Parameterized expense")
                    .amount(total)
                    .currency("TWD")
                    .expenseDate(LocalDate.of(2024, 3, 5))
                    .build();

            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(expense));

            TripMember member = TripMember.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .userId(userId)
                    .role(Role.EDITOR)
                    .personalBudget(budget)
                    .build();
            when(tripMemberRepository.findByTripIdAndUserId(tripId, userId))
                    .thenReturn(Optional.of(member));

            PersonalExpenseSummaryResponse result =
                    personalExpenseService.getPersonalSummary(userId, tripId);

            BudgetStatus expected = BudgetStatus.valueOf(expectedStatus);
            assertThat(result.getBudgetStatus()).isEqualTo(expected);
        }

        @Test
        @DisplayName("GREEN status has null budgetOverage")
        void getPersonalSummary_green_budgetOverageIsNull() {
            setupMemberAndTrip();

            PersonalExpense expense = PersonalExpense.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .tripId(tripId)
                    .description("Small expense")
                    .amount(new BigDecimal("500.00"))
                    .currency("TWD")
                    .expenseDate(LocalDate.of(2024, 3, 5))
                    .build();

            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(expense));

            TripMember member = TripMember.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .userId(userId)
                    .role(Role.OWNER)
                    .personalBudget(new BigDecimal("1000.00"))
                    .build();
            when(tripMemberRepository.findByTripIdAndUserId(tripId, userId))
                    .thenReturn(Optional.of(member));

            PersonalExpenseSummaryResponse result =
                    personalExpenseService.getPersonalSummary(userId, tripId);

            assertThat(result.getBudgetStatus()).isEqualTo(BudgetStatus.GREEN);
            assertThat(result.getBudgetOverage()).isNull();
        }

        @Test
        @DisplayName("YELLOW status has null budgetOverage")
        void getPersonalSummary_yellow_budgetOverageIsNull() {
            setupMemberAndTrip();

            PersonalExpense expense = PersonalExpense.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .tripId(tripId)
                    .description("Mid expense")
                    .amount(new BigDecimal("850.00"))
                    .currency("TWD")
                    .expenseDate(LocalDate.of(2024, 3, 5))
                    .build();

            when(personalExpenseRepository.findByUserIdAndTripId(userId, tripId))
                    .thenReturn(List.of(expense));

            TripMember member = TripMember.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .userId(userId)
                    .role(Role.OWNER)
                    .personalBudget(new BigDecimal("1000.00"))
                    .build();
            when(tripMemberRepository.findByTripIdAndUserId(tripId, userId))
                    .thenReturn(Optional.of(member));

            PersonalExpenseSummaryResponse result =
                    personalExpenseService.getPersonalSummary(userId, tripId);

            assertThat(result.getBudgetStatus()).isEqualTo(BudgetStatus.YELLOW);
            assertThat(result.getBudgetOverage()).isNull();
        }
    }

    // ========== createPersonalExpense ==========

    @Nested
    @DisplayName("createPersonalExpense")
    class CreatePersonalExpenseTests {

        @Test
        @DisplayName("saved expense has userId matching the passed userId")
        void createPersonalExpense_ownership_savedWithCorrectUserId() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));

            CreatePersonalExpenseRequest request = CreatePersonalExpenseRequest.builder()
                    .description("Lunch")
                    .amount(new BigDecimal("150.00"))
                    .currency("TWD")
                    .expenseDate(LocalDate.of(2024, 3, 10))
                    .build();

            ArgumentCaptor<PersonalExpense> captor = ArgumentCaptor.forClass(PersonalExpense.class);
            PersonalExpense savedExpense = PersonalExpense.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .tripId(tripId)
                    .description("Lunch")
                    .amount(new BigDecimal("150.00"))
                    .currency("TWD")
                    .expenseDate(LocalDate.of(2024, 3, 10))
                    .build();
            when(personalExpenseRepository.save(any(PersonalExpense.class))).thenReturn(savedExpense);

            personalExpenseService.createPersonalExpense(userId, tripId, request);

            verify(personalExpenseRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(userId);
            assertThat(captor.getValue().getTripId()).isEqualTo(tripId);
        }

        @Test
        @DisplayName("expenseDate outside trip range throws ValidationException")
        void createPersonalExpense_dateOutsideTripRange_throwsValidationException() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));

            CreatePersonalExpenseRequest request = CreatePersonalExpenseRequest.builder()
                    .description("Out of range")
                    .amount(new BigDecimal("100.00"))
                    .currency("TWD")
                    // Trip ends 2024-03-31; this is after endDate
                    .expenseDate(LocalDate.of(2024, 4, 1))
                    .build();

            assertThatThrownBy(() ->
                    personalExpenseService.createPersonalExpense(userId, tripId, request))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("expenseDate before trip start throws ValidationException")
        void createPersonalExpense_dateBeforeTripStart_throwsValidationException() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));

            CreatePersonalExpenseRequest request = CreatePersonalExpenseRequest.builder()
                    .description("Too early")
                    .amount(new BigDecimal("100.00"))
                    .currency("TWD")
                    // Trip starts 2024-03-01; this is before startDate
                    .expenseDate(LocalDate.of(2024, 2, 28))
                    .build();

            assertThatThrownBy(() ->
                    personalExpenseService.createPersonalExpense(userId, tripId, request))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("same currency as baseCurrency stores null exchangeRate")
        void createPersonalExpense_sameCurrency_exchangeRateIsNull() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));

            CreatePersonalExpenseRequest request = CreatePersonalExpenseRequest.builder()
                    .description("Lunch")
                    .amount(new BigDecimal("150.00"))
                    .currency("TWD") // same as mockTrip.baseCurrency
                    .expenseDate(LocalDate.of(2024, 3, 10))
                    .build();

            ArgumentCaptor<PersonalExpense> captor = ArgumentCaptor.forClass(PersonalExpense.class);
            PersonalExpense savedExpense = PersonalExpense.builder()
                    .id(UUID.randomUUID()).userId(userId).tripId(tripId)
                    .description("Lunch").amount(new BigDecimal("150.00"))
                    .currency("TWD").expenseDate(LocalDate.of(2024, 3, 10))
                    .build();
            when(personalExpenseRepository.save(any(PersonalExpense.class))).thenReturn(savedExpense);

            personalExpenseService.createPersonalExpense(userId, tripId, request);

            verify(personalExpenseRepository).save(captor.capture());
            assertThat(captor.getValue().getExchangeRate()).isNull();
            verify(exchangeRateService, never()).getRate(any(), any());
        }

        @Test
        @DisplayName("foreign currency auto-fetches exchangeRate from ExchangeRateService")
        void createPersonalExpense_foreignCurrency_exchangeRateAutoFetched() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
            when(exchangeRateService.getRate("JPY", "TWD"))
                    .thenReturn(ExchangeRateResponse.fresh("JPY", "TWD",
                            new BigDecimal("0.2234"), java.time.Instant.now()));

            CreatePersonalExpenseRequest request = CreatePersonalExpenseRequest.builder()
                    .description("Ramen")
                    .amount(new BigDecimal("1000"))
                    .currency("JPY")
                    .expenseDate(LocalDate.of(2024, 3, 10))
                    .build();

            ArgumentCaptor<PersonalExpense> captor = ArgumentCaptor.forClass(PersonalExpense.class);
            PersonalExpense savedExpense = PersonalExpense.builder()
                    .id(UUID.randomUUID()).userId(userId).tripId(tripId)
                    .description("Ramen").amount(new BigDecimal("1000"))
                    .currency("JPY").exchangeRate(new BigDecimal("0.223400"))
                    .expenseDate(LocalDate.of(2024, 3, 10))
                    .build();
            when(personalExpenseRepository.save(any(PersonalExpense.class))).thenReturn(savedExpense);

            personalExpenseService.createPersonalExpense(userId, tripId, request);

            verify(personalExpenseRepository).save(captor.capture());
            assertThat(captor.getValue().getExchangeRate())
                    .isEqualByComparingTo(new BigDecimal("0.223400"));
        }

        @Test
        @DisplayName("manual exchangeRate in request overrides auto-fetch")
        void createPersonalExpense_manualRate_usesManualRate() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));

            CreatePersonalExpenseRequest request = CreatePersonalExpenseRequest.builder()
                    .description("Taxi")
                    .amount(new BigDecimal("50"))
                    .currency("USD")
                    .exchangeRate(new BigDecimal("31.5")) // manual rate
                    .expenseDate(LocalDate.of(2024, 3, 10))
                    .build();

            ArgumentCaptor<PersonalExpense> captor = ArgumentCaptor.forClass(PersonalExpense.class);
            PersonalExpense savedExpense = PersonalExpense.builder()
                    .id(UUID.randomUUID()).userId(userId).tripId(tripId)
                    .description("Taxi").amount(new BigDecimal("50"))
                    .currency("USD").exchangeRate(new BigDecimal("31.500000"))
                    .expenseDate(LocalDate.of(2024, 3, 10))
                    .build();
            when(personalExpenseRepository.save(any(PersonalExpense.class))).thenReturn(savedExpense);

            personalExpenseService.createPersonalExpense(userId, tripId, request);

            verify(personalExpenseRepository).save(captor.capture());
            assertThat(captor.getValue().getExchangeRate())
                    .isEqualByComparingTo(new BigDecimal("31.500000"));
            verify(exchangeRateService, never()).getRate(any(), any());
        }

        @Test
        @DisplayName("API failure stores null exchangeRate gracefully")
        void createPersonalExpense_apiFails_exchangeRateIsNull() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
            when(exchangeRateService.getRate("USD", "TWD"))
                    .thenThrow(new RuntimeException("API unavailable"));

            CreatePersonalExpenseRequest request = CreatePersonalExpenseRequest.builder()
                    .description("Coffee")
                    .amount(new BigDecimal("5"))
                    .currency("USD")
                    .expenseDate(LocalDate.of(2024, 3, 10))
                    .build();

            ArgumentCaptor<PersonalExpense> captor = ArgumentCaptor.forClass(PersonalExpense.class);
            PersonalExpense savedExpense = PersonalExpense.builder()
                    .id(UUID.randomUUID()).userId(userId).tripId(tripId)
                    .description("Coffee").amount(new BigDecimal("5"))
                    .currency("USD").expenseDate(LocalDate.of(2024, 3, 10))
                    .build();
            when(personalExpenseRepository.save(any(PersonalExpense.class))).thenReturn(savedExpense);

            personalExpenseService.createPersonalExpense(userId, tripId, request);

            verify(personalExpenseRepository).save(captor.capture());
            assertThat(captor.getValue().getExchangeRate()).isNull();
        }

        @Test
        @DisplayName("null expenseDate is allowed (no date validation performed)")
        void createPersonalExpense_nullDate_successfullyCreates() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));

            CreatePersonalExpenseRequest request = CreatePersonalExpenseRequest.builder()
                    .description("Misc")
                    .amount(new BigDecimal("100.00"))
                    .currency("TWD")
                    .expenseDate(null)
                    .build();

            PersonalExpense savedExpense = PersonalExpense.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .tripId(tripId)
                    .description("Misc")
                    .amount(new BigDecimal("100.00"))
                    .currency("TWD")
                    .build();
            when(personalExpenseRepository.save(any(PersonalExpense.class))).thenReturn(savedExpense);

            PersonalExpenseItemResponse result =
                    personalExpenseService.createPersonalExpense(userId, tripId, request);

            assertThat(result).isNotNull();
            assertThat(result.getSource()).isEqualTo(Source.MANUAL);
        }
    }

    // ========== updatePersonalExpense ==========

    @Nested
    @DisplayName("updatePersonalExpense")
    class UpdatePersonalExpenseTests {

        @Test
        @DisplayName("updating another user's expense throws ForbiddenException")
        void updatePersonalExpense_otherUsersExpense_throwsForbiddenException() {
            UUID expenseId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            // Expense belongs to otherUserId, but current user is userId
            PersonalExpense expense = PersonalExpense.builder()
                    .id(expenseId)
                    .userId(otherUserId)
                    .tripId(tripId)
                    .description("Other's expense")
                    .amount(new BigDecimal("100.00"))
                    .currency("TWD")
                    .build();

            when(personalExpenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);

            UpdatePersonalExpenseRequest request = UpdatePersonalExpenseRequest.builder()
                    .description("Trying to update")
                    .build();

            assertThatThrownBy(() ->
                    personalExpenseService.updatePersonalExpense(expenseId, userId, request))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("changing currency re-resolves exchangeRate")
        void updatePersonalExpense_currencyChanged_rateReResolved() {
            UUID expenseId = UUID.randomUUID();

            PersonalExpense expense = PersonalExpense.builder()
                    .id(expenseId).userId(userId).tripId(tripId)
                    .description("Old expense").amount(new BigDecimal("100.00"))
                    .currency("TWD").exchangeRate(null)
                    .build();

            when(personalExpenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
            when(exchangeRateService.getRate("JPY", "TWD"))
                    .thenReturn(ExchangeRateResponse.fresh("JPY", "TWD",
                            new BigDecimal("0.2200"), java.time.Instant.now()));
            when(personalExpenseRepository.save(any(PersonalExpense.class))).thenReturn(expense);

            UpdatePersonalExpenseRequest request = UpdatePersonalExpenseRequest.builder()
                    .currency("JPY")
                    .build();

            personalExpenseService.updatePersonalExpense(expenseId, userId, request);

            verify(exchangeRateService).getRate("JPY", "TWD");
            verify(personalExpenseRepository).save(expense);
            assertThat(expense.getExchangeRate()).isEqualByComparingTo(new BigDecimal("0.220000"));
        }

        @Test
        @DisplayName("changing only amount does not overwrite exchangeRate")
        void updatePersonalExpense_amountOnly_rateUnchanged() {
            UUID expenseId = UUID.randomUUID();
            BigDecimal existingRate = new BigDecimal("0.220000");

            PersonalExpense expense = PersonalExpense.builder()
                    .id(expenseId).userId(userId).tripId(tripId)
                    .description("JPY expense").amount(new BigDecimal("1000"))
                    .currency("JPY").exchangeRate(existingRate)
                    .build();

            when(personalExpenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
            when(personalExpenseRepository.save(any(PersonalExpense.class))).thenReturn(expense);

            UpdatePersonalExpenseRequest request = UpdatePersonalExpenseRequest.builder()
                    .amount(new BigDecimal("2000"))
                    .build();

            personalExpenseService.updatePersonalExpense(expenseId, userId, request);

            // Rate should remain unchanged
            assertThat(expense.getExchangeRate()).isEqualByComparingTo(existingRate);
        }

        @Test
        @DisplayName("updating own expense with valid date succeeds")
        void updatePersonalExpense_ownExpenseValidDate_succeeds() {
            UUID expenseId = UUID.randomUUID();

            PersonalExpense expense = PersonalExpense.builder()
                    .id(expenseId)
                    .userId(userId)
                    .tripId(tripId)
                    .description("My expense")
                    .amount(new BigDecimal("100.00"))
                    .currency("TWD")
                    .build();

            when(personalExpenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);
            when(tripRepository.findById(tripId)).thenReturn(Optional.of(mockTrip));
            when(personalExpenseRepository.save(any(PersonalExpense.class))).thenReturn(expense);

            UpdatePersonalExpenseRequest request = UpdatePersonalExpenseRequest.builder()
                    .description("Updated description")
                    .expenseDate(LocalDate.of(2024, 3, 15))
                    .build();

            PersonalExpenseItemResponse result =
                    personalExpenseService.updatePersonalExpense(expenseId, userId, request);

            assertThat(result).isNotNull();
            verify(personalExpenseRepository).save(expense);
        }
    }

    // ========== deletePersonalExpense ==========

    @Nested
    @DisplayName("deletePersonalExpense")
    class DeletePersonalExpenseTests {

        @Test
        @DisplayName("deleting another user's expense throws ForbiddenException")
        void deletePersonalExpense_otherUsersExpense_throwsForbiddenException() {
            UUID expenseId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            PersonalExpense expense = PersonalExpense.builder()
                    .id(expenseId)
                    .userId(otherUserId)
                    .tripId(tripId)
                    .description("Other's expense")
                    .amount(new BigDecimal("100.00"))
                    .currency("TWD")
                    .build();

            when(personalExpenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);

            assertThatThrownBy(() ->
                    personalExpenseService.deletePersonalExpense(expenseId, userId))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("deleting own expense succeeds")
        void deletePersonalExpense_ownExpense_deletesSuccessfully() {
            UUID expenseId = UUID.randomUUID();

            PersonalExpense expense = PersonalExpense.builder()
                    .id(expenseId)
                    .userId(userId)
                    .tripId(tripId)
                    .description("My expense")
                    .amount(new BigDecimal("100.00"))
                    .currency("TWD")
                    .build();

            when(personalExpenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);

            personalExpenseService.deletePersonalExpense(expenseId, userId);

            verify(personalExpenseRepository).delete(expense);
        }
    }

    // ========== setPersonalBudget ==========

    @Nested
    @DisplayName("setPersonalBudget")
    class SetPersonalBudgetTests {

        @Test
        @DisplayName("negative budget throws ValidationException with INVALID_BUDGET code")
        void setPersonalBudget_negativeBudget_throwsValidationException() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);

            SetPersonalBudgetRequest request = SetPersonalBudgetRequest.builder()
                    .budget(new BigDecimal("-100.00"))
                    .build();

            assertThatThrownBy(() ->
                    personalExpenseService.setPersonalBudget(tripId, userId, request))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("預算");
        }

        @Test
        @DisplayName("budget = 0 throws ValidationException")
        void setPersonalBudget_zeroBudget_throwsValidationException() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);

            SetPersonalBudgetRequest request = SetPersonalBudgetRequest.builder()
                    .budget(BigDecimal.ZERO)
                    .build();

            assertThatThrownBy(() ->
                    personalExpenseService.setPersonalBudget(tripId, userId, request))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("valid positive budget saves TripMember with updated personalBudget")
        void setPersonalBudget_validBudget_savesTripMemberWithNewBudget() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(true);

            TripMember member = TripMember.builder()
                    .id(UUID.randomUUID())
                    .tripId(tripId)
                    .userId(userId)
                    .role(Role.OWNER)
                    .personalBudget(null)
                    .build();

            when(tripMemberRepository.findByTripIdAndUserId(tripId, userId))
                    .thenReturn(Optional.of(member));

            BigDecimal newBudget = new BigDecimal("5000.00");
            SetPersonalBudgetRequest request = SetPersonalBudgetRequest.builder()
                    .budget(newBudget)
                    .build();

            personalExpenseService.setPersonalBudget(tripId, userId, request);

            ArgumentCaptor<TripMember> captor = ArgumentCaptor.forClass(TripMember.class);
            verify(tripMemberRepository).save(captor.capture());
            assertThat(captor.getValue().getPersonalBudget()).isEqualByComparingTo(newBudget);
        }

        @Test
        @DisplayName("non-member throws ForbiddenException before budget validation")
        void setPersonalBudget_nonMember_throwsForbiddenException() {
            when(permissionChecker.isMember(tripId, userId)).thenReturn(false);

            SetPersonalBudgetRequest request = SetPersonalBudgetRequest.builder()
                    .budget(new BigDecimal("1000.00"))
                    .build();

            assertThatThrownBy(() ->
                    personalExpenseService.setPersonalBudget(tripId, userId, request))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
