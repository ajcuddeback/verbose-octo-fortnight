package com.budgetapp.service;

import com.budgetapp.dto.response.PredictionResponse;
import com.budgetapp.dto.response.SpendingDnaResponse;
import com.budgetapp.dto.response.SpendingTrendResponse;
import com.budgetapp.model.*;
import com.budgetapp.repository.TransactionRepository;
import com.budgetapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User testUser;
    private Category foodCategory;
    private Category salaryCategory;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .subscriptionStatus(SubscriptionStatus.ACTIVE)
                .build();

        foodCategory = Category.builder()
                .id(1L)
                .name("Food & Dining")
                .type(TransactionType.EXPENSE)
                .colorHex("#FF5722")
                .build();

        salaryCategory = Category.builder()
                .id(2L)
                .name("Salary")
                .type(TransactionType.INCOME)
                .colorHex("#4CAF50")
                .build();
    }

    @Test
    void getSpendingTrends_ReturnsCorrectMonthlyData() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        YearMonth currentMonth = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            LocalDate start = targetMonth.atDay(1);
            LocalDate end = targetMonth.atEndOfMonth();

            Transaction income = Transaction.builder()
                    .user(testUser)
                    .amount(new BigDecimal("3000.00"))
                    .type(TransactionType.INCOME)
                    .transactionDate(start.plusDays(1))
                    .build();

            Transaction expense = Transaction.builder()
                    .user(testUser)
                    .amount(new BigDecimal("2000.00"))
                    .type(TransactionType.EXPENSE)
                    .transactionDate(start.plusDays(5))
                    .build();

            when(transactionRepository.findByUserAndTransactionDateBetween(testUser, start, end))
                    .thenReturn(List.of(income, expense));
        }

        // Act
        SpendingTrendResponse response = analyticsService.getSpendingTrends(1L, 6);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getDataPoints()).hasSize(6);

        SpendingTrendResponse.MonthlyDataPoint firstPoint = response.getDataPoints().get(0);
        assertThat(firstPoint.getTotalIncome()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(firstPoint.getTotalExpenses()).isEqualByComparingTo(new BigDecimal("2000.00"));
        assertThat(firstPoint.getNetSavings()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void getSpendingTrends_EmptyTransactions_ReturnsZeroValues() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserAndTransactionDateBetween(
                eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        // Act
        SpendingTrendResponse response = analyticsService.getSpendingTrends(1L, 3);

        // Assert
        assertThat(response.getDataPoints()).hasSize(3);
        response.getDataPoints().forEach(point -> {
            assertThat(point.getTotalIncome()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(point.getTotalExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(point.getNetSavings()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    void getPredictions_WithMockData_ReturnsValidPrediction() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        YearMonth currentMonth = YearMonth.now();
        // Provide increasing expense data for 6 months
        BigDecimal[] expenses = {
                new BigDecimal("1000"), new BigDecimal("1100"),
                new BigDecimal("1200"), new BigDecimal("1300"),
                new BigDecimal("1400"), new BigDecimal("1500")
        };

        for (int i = 6; i >= 1; i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);
            LocalDate start = targetMonth.atDay(1);
            LocalDate end = targetMonth.atEndOfMonth();

            int index = 6 - i;
            Transaction expense = Transaction.builder()
                    .user(testUser)
                    .amount(expenses[index])
                    .type(TransactionType.EXPENSE)
                    .transactionDate(start.plusDays(1))
                    .build();

            Transaction income = Transaction.builder()
                    .user(testUser)
                    .amount(new BigDecimal("3000.00"))
                    .type(TransactionType.INCOME)
                    .transactionDate(start.plusDays(1))
                    .build();

            when(transactionRepository.findByUserAndTransactionDateBetween(testUser, start, end))
                    .thenReturn(List.of(expense, income));
        }

        // Act
        PredictionResponse response = analyticsService.getPredictions(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getPredictedExpenses()).isNotNull();
        assertThat(response.getPredictedIncome()).isNotNull();
        assertThat(response.getBasedOnMonths()).isEqualTo(6);
        assertThat(response.getConfidence()).isBetween(0.0, 1.0);
        assertThat(response.getMonthlyBreakdown()).hasSize(3);
        // With increasing trend, predicted value should be greater than 1500
        assertThat(response.getPredictedExpenses()).isGreaterThan(new BigDecimal("1500"));
    }

    @Test
    void getPredictions_NoData_ReturnsZeroPrediction() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserAndTransactionDateBetween(
                eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        // Act
        PredictionResponse response = analyticsService.getPredictions(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getPredictedExpenses()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getPredictedIncome()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getSpendingDna_FoodTopCategory_ReturnsFoodEnthusiast() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        Transaction foodExpense1 = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("500.00"))
                .type(TransactionType.EXPENSE)
                .category(foodCategory)
                .transactionDate(LocalDate.now().minusDays(10))
                .build();

        Transaction foodExpense2 = Transaction.builder()
                .user(testUser)
                .amount(new BigDecimal("300.00"))
                .type(TransactionType.EXPENSE)
                .category(foodCategory)
                .transactionDate(LocalDate.now().minusDays(20))
                .build();

        when(transactionRepository.findByUserAndTransactionDateBetween(
                eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(foodExpense1, foodExpense2));

        // Act
        SpendingDnaResponse response = analyticsService.getSpendingDna(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getPersonalityType()).isEqualTo("Food Enthusiast");
        assertThat(response.getPersonalityDescription()).isNotBlank();
        assertThat(response.getTopCategories()).isNotEmpty();
        assertThat(response.getInsights()).isNotEmpty();
    }

    @Test
    void getSpendingDna_NoTransactions_ReturnsBalancedSpender() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserAndTransactionDateBetween(
                eq(testUser), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());

        // Act
        SpendingDnaResponse response = analyticsService.getSpendingDna(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getPersonalityType()).isEqualTo("Balanced Spender");
        assertThat(response.getTopCategories()).isEmpty();
    }
}
